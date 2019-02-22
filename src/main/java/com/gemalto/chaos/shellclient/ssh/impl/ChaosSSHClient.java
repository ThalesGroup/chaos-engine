package com.gemalto.chaos.shellclient.ssh.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.SSHConstants;
import com.gemalto.chaos.shellclient.ssh.SSHClientWrapper;
import com.gemalto.chaos.shellclient.ssh.SSHCredentials;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Objects;

public class ChaosSSHClient implements SSHClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(ChaosSSHClient.class);
    private SSHClient sshClient;
    private SSHCredentials sshCredentials;
    private String hostname;
    private Integer port;

    @Override
    public SSHClientWrapper connect () {
        boolean opened = false;
        Objects.requireNonNull(hostname);
        Objects.requireNonNull(port);
        Objects.requireNonNull(sshCredentials);
        sshClient = buildNewSSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            sshClient.connect(hostname, port);
        } catch (IOException e) {
            throw new ChaosException(e);
        }
        try {
            sshClient.auth(sshCredentials.getUsername(), sshCredentials.getAuthMethods());
            opened = true;
        } catch (UserAuthException e) {
            log.error("Authentication failure connecting to SSH", e);
            throw new ChaosException(e);
        } catch (TransportException e) {
            log.error("Networking exception connecting to SSH", e);
            throw new ChaosException(e);
        } finally {
            if (!opened) {
                try {
                    close();
                } catch (IOException e) {
                    throw new ChaosException("Error closing failed SSH connection", e);
                }
            }
        }
        return this;
    }

    SSHClient buildNewSSHClient () {
        return new SSHClient();
    }

    @Override
    public void close () throws IOException {
        try {
            getSshClient().close();
        } catch (ChaosException ignored) {
            // Ignore the Exception that getSSHClient can throw here.
        }
    }

    SSHClient getSshClient () {
        if (sshClient == null) {
            throw new ChaosException("Attempted to use an SSH Client that was not instantiated");
        }
        return sshClient;
    }

    @Override
    public SSHClientWrapper withSSHCredentials (SSHCredentials sshCredentials) {
        this.sshCredentials = sshCredentials;
        return this;
    }

    @Override
    public SSHClientWrapper withEndpoint (String hostname) {
        return withEndpoint(hostname, SSHConstants.DEFAULT_SSH_PORT);
    }

    @Override
    public SSHClientWrapper withEndpoint (String hostname, Integer portNumber) {
        if (portNumber <= 0 || portNumber > 65535) {
            throw new IllegalArgumentException("Port Number should be valid TCP Port between 1 and 65535");
        }
        this.hostname = hostname;
        this.port = portNumber;
        return this;
    }

    @Override
    public String runCommand (String command) {
        try (Session session = getSshClient().startSession()) {
            try (Session.Command exec = session.exec(command)) {
                exec.join();
                int exitCode = exec.getExitStatus();
                String output = IOUtils.readFully(exec.getInputStream()).toString();
                if (exitCode != 0) {
                    throw new ChaosException("Received a non-zero exit code when running SSH Command");
                }
                return output;
            }
        } catch (IOException e) {
            log.error("Error running SSH Command", e);
            throw new ChaosException(e);
        }
    }

    @Override
    public String runResource (Resource resource) {
        try (Session session = getSshClient().startSession()) {
            getSshClient().newSCPFileTransfer()
                          .upload(resource.getFile().getAbsolutePath(), SSHConstants.TEMP_DIRECTORY);
            try (Session.Command exec = session.exec("source " + SSHConstants.TEMP_DIRECTORY + resource.getFilename())) {
                exec.join();
                int exitCode = exec.getExitStatus();
                String output = IOUtils.readFully(exec.getInputStream()).toString();
                if (exitCode != 0) {
                    throw new ChaosException("Received a non-zero exit code when running SSH Command");
                }
                return output;
            }
        } catch (IOException e) {
            log.error("Error running SSH Command", e);
            throw new ChaosException(e);
        }
    }
}
