package com.gemalto.chaos.shellclient.ssh.impl;

import com.gemalto.chaos.constants.SSHConstants;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.shellclient.ShellClient;
import com.gemalto.chaos.shellclient.ShellOutput;
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
import java.util.concurrent.*;

import static com.gemalto.chaos.exception.enums.ChaosErrorCode.*;
import static net.logstash.logback.argument.StructuredArguments.v;

public class ChaosSSHClient implements SSHClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(ChaosSSHClient.class);
    private SSHClient sshClient;
    private SSHCredentials sshCredentials;
    private String hostname;
    private Integer port;

    @Override
    public ShellClient connect (int connectionTimeout) throws IOException {
        boolean opened = false;
        Objects.requireNonNull(hostname);
        Objects.requireNonNull(port);
        Objects.requireNonNull(sshCredentials);
        log.debug("Creating connection to {}:{} with {} second timeout", hostname, port, connectionTimeout);
        sshClient = buildNewSSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.setConnectTimeout(connectionTimeout);
        Future<Void> connection = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                sshClient.connect(hostname, port);
                return null;
            } catch (IOException e) {
                throw new ChaosException(SHELL_CLIENT_CONNECT_FAILURE, e);
            }
        });
        try {
            connection.get(connectionTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChaosException(SHELL_CLIENT_CONNECT_FAILURE, e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to connect within the timeout", e);
            throw new ChaosException(SHELL_CLIENT_CONNECT_FAILURE, e);
        }
        try {
            log.debug("Connection made, sending authentication");
            sshClient.auth(sshCredentials.getUsername(), sshCredentials.getAuthMethods());
            opened = true;
        } catch (UserAuthException e) {
            log.error("Authentication failure connecting to SSH", e);
            throw new ChaosException(SHELL_CLIENT_CONNECT_FAILURE, e);
        } catch (TransportException e) {
            log.error("Networking exception connecting to SSH", e);
            throw new ChaosException(SHELL_CLIENT_CONNECT_FAILURE, e);
        } finally {
            if (!opened) {
                close();
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
            throw new ChaosException(SSH_CLIENT_INSTANTIATION_ERROR);
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
        int chosenPort = SSHConstants.DEFAULT_SSH_PORT;
        if (hostname.matches(".*:[1-9][0-9]*$")) {
            log.debug("Parsing hostname with port {}", hostname);
            int split = hostname.lastIndexOf(':');
            chosenPort = Integer.parseInt(hostname.substring(split + 1));
            hostname = hostname.substring(0, split);
            log.debug("Evaluated as {} port {}", hostname, chosenPort);
        }
        return withEndpoint(hostname, chosenPort);
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
    public String runResource (Resource resource) {
        try {
            log.info("Transferring resource {}", resource);
            getSshClient().newSCPFileTransfer()
                          .upload(new JarResourceFile(resource, true), SSHConstants.TEMP_DIRECTORY);
        } catch (IOException e) {
            throw new ChaosException(SSH_CLIENT_TRANSFER_ERROR, e);
        }
        try (Session session = getSshClient().startSession()) {
            String shellCommand = SSHConstants.TEMP_DIRECTORY + resource.getFilename();
            log.debug("About to run {}", shellCommand);
            return runCommandInShell(session, shellCommand);
        } catch (IOException e) {
            log.error("Error running SSH Command", e);
            throw new ChaosException(SSH_CLIENT_COMMAND_ERROR, e);
        }
    }

    @Override
    public ShellOutput runCommand (String command) {
        log.info("About to run command {}", command);
        try (Session session = getSshClient().startSession()) {
            session.allocateDefaultPTY();
            try (Session.Command exec = session.exec(command)) {
                exec.join();
                int exitCode = exec.getExitStatus();
                String output = IOUtils.readFully(exec.getInputStream()).toString();
                ShellOutput shellOutput = ShellOutput.builder().withExitCode(exitCode).withStdOut(output).build();
                if (exitCode > 0) {
                    log.debug("Command execution failed {}", v("failure", shellOutput));
                }
                return shellOutput;
            }
        } catch (IOException e) {
            log.error("Error running SSH Command", e);
            throw new ChaosException(SSH_CLIENT_COMMAND_ERROR, e);
        }
    }

    String runCommandInShell (Session session, String command) {
        try {
            try (Session.Command shell = session.exec(String.format(SSHConstants.SCRIPT_NOHUP_WRAPPER, command))) {
                shell.join();
                return IOUtils.readFully(shell.getInputStream()).toString();
            }
        } catch (IOException e) {
            throw new ChaosException(SSH_CLIENT_COMMAND_ERROR, e);
        }
    }
}
