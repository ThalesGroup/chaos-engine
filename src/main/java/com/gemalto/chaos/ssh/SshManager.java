package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SshManager {
    private static final Logger log = LoggerFactory.getLogger(SshManager.class);
    private SSHClient ssh = new SSHClient();
    private String hostname;
    private String port;

    public SshManager (String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
    }

    public boolean connect (String userName, String password) {
        try {
            log.debug("Connecting to {}", hostname);

            ssh.connect(hostname, Integer.valueOf(port));
            ssh.authPassword(userName, password);
            if (ssh.isConnected() && ssh.isAuthenticated()) {
                log.debug("Connection to {} succeeded.", hostname);
                return true;
            } else {
                log.error("SSH Authentication failed.");
            }

        } catch (IOException e) {
            log.error("Unable to connect to {}: {}", hostname, e.getMessage());
        }
        return false;
    }

    public SshCommandResult executeCommand (String command) {
        try {
            Session session = ssh.startSession();
            log.debug("Going to execute command: {}", command);
            Command cmd = session.exec(command);
            cmd.join(60, TimeUnit.SECONDS);
            SshCommandResult result = new SshCommandResult(cmd);
            log.debug("Command execution finished with exit code: {}", cmd.getExitStatus());
            session.close();
            return result;
        } catch (SSHException e) {
            log.error("Unable to execute command '{}' on {}: {}", command, hostname, e.getMessage());
        }
        return new SshCommandResult(null, -1);
    }

    public void disconnect () {
        try {
            ssh.disconnect();
        } catch (IOException e) {
            log.error("Disconnect from {} failed: {}", hostname, e.getMessage());
        }
    }

    public ShellSessionCapabilityProvider getShellCapabilities () {
        ShellSessionCapabilityProvider capabilities = new ShellSessionCapabilityProvider(this);
        capabilities.build();
        return capabilities;
    }
}
