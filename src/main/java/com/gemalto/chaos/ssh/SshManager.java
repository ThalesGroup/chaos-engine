package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SshManager {
    private static final Logger log = LoggerFactory.getLogger(SshManager.class);
    private SSHClient ssh = new SSHClient();
    private Session session;
    private String hostname;
    private String port;

    public SshManager (String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
    }

    public Session connect (String userName, String password) {
        try {
            log.debug("Connecting to {}", hostname);

            ssh.connect(hostname, Integer.valueOf(port));
            ssh.authPassword(userName, password);
            if (ssh.isConnected() && ssh.isAuthenticated()) {
                session = ssh.startSession();
                log.debug("Connection to {} succeeded.", hostname);
            } else {
                log.error("SSH Authentication failed.");
            }
            return session;
        } catch (IOException e) {
            log.error("Unable to connect to {}: {}", hostname, e.getMessage());
        }
        return null;
    }

    public SshCommandResult executeCommand (String command) {
        try {
            Command cmd = session.exec(command);
            return new SshCommandResult(cmd);
        } catch (SSHException e) {
            log.error("Unable to execute command '{}' on {}: {}", command, hostname, e.getMessage());
        }
        return new SshCommandResult(null, -1);
    }

    public void disconnect () {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
            ssh.disconnect();
        } catch (IOException e) {
            log.error("Disconnect from {} failed: {}", hostname, e.getMessage());
        }
    }

    public ShellSessionCapabilities getShellCapabilities () {
        ShellSessionCapabilities capabilities = new ShellSessionCapabilities(this);
        return capabilities;
    }
}
