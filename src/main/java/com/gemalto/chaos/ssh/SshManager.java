package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            log.debug("Connecting to host {}", hostname);
            ssh.connect(hostname, Integer.valueOf(port));
            ssh.authPassword(userName, password);
            if (ssh.isConnected() && ssh.isAuthenticated()) {
                log.debug("Connection to host {} succeeded.", hostname);
                return true;
            } else {
                log.error("SSH Authentication failed.");
            }
        } catch (IOException e) {
            log.error("Unable to connect to {}: {}", hostname, e.getMessage());
        }
        return false;
    }

    public void executeCommandInInteractiveShell (String command, String shellName, int maxSessionDuration) {
        try {
            Session session = ssh.startSession();
            session.allocateDefaultPTY();
            log.debug("Going to execute command {} in interactive shell session {}", command, shellName);
            Session.Shell shell = session.startShell();
            shell.setAutoExpand(true);
            OutputStream outputStream = shell.getOutputStream();
            //Interactive command must be ended by new line
            outputStream.write(Strings.toUTF8ByteArray(command + "\n"));
            outputStream.flush();
            InputStream stream = shell.getInputStream();
            //Interactive session must be ended by exit command or ssh connection drop
            //ttl is there for security reasons when the library don't recognize disconnected channel
            //can happen when aggressive attacks are performed
            int ttl = 0;
            while (ssh.isConnected() && !shell.isEOF() && !session.isEOF() && ttl > maxSessionDuration) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("Interactive SSH session {} is still active", shellName);
            }
            log.debug("Interactive SSH session {} has ended. Closing the shell", shellName);
            if (session.isOpen()) {
                session.close();
            }
            if (shell.isOpen()) {
                shell.close();
            }
            log.debug("Interactive shell session ended.");
        } catch (IOException e) {
            log.error("Unable to execute command '{}' on {}: {}", command, hostname, e.getMessage());
            e.printStackTrace();
        }
    }

    public SshCommandResult executeCommand (String command) {
        try {
            Session session = ssh.startSession();
            session.allocateDefaultPTY();
            log.debug("Going to execute command: {}", command);
            Command cmd = session.exec(command);
            cmd.join(120, TimeUnit.SECONDS);
            SshCommandResult result = new SshCommandResult(cmd);
            session.close();
            log.debug("Command execution finished with exit code: {}", cmd.getExitStatus());
            return result;
        } catch (SSHException e) {
            log.error("Unable to execute command '{}' on {}: {}", command, hostname, e.getMessage());
        }
        return new SshCommandResult(null, -1);
    }

    public void disconnect () {
        try {
            if (ssh.isConnected()) {
                ssh.disconnect();
            }
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
