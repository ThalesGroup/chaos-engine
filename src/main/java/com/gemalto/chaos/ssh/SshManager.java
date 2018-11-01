package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
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
    private SSHClient sshClient = new SSHClient();
    private String hostname;
    private String port;

    public SshManager (String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
    }

    void setSshClient (SSHClient sshClient) {
        this.sshClient = sshClient;
    }
    public boolean connect (String userName, String password) throws IOException{
            log.debug("Connecting to host {}", hostname);
            sshClient.connect(hostname, Integer.valueOf(port));
            sshClient.authPassword(userName, password);
            if (sshClient.isConnected() && sshClient.isAuthenticated()) {
                log.debug("Connection to host {} succeeded.", hostname);
                return true;
            } else {
                log.error("SSH Authentication failed.");
            }
        return false;
    }

    public void executeCommandInInteractiveShell (String command, String shellName, int maxSessionDuration) throws IOException{
            Session session = sshClient.startSession();
            session.allocateDefaultPTY();
            log.debug("Going to execute command {} in interactive shell session {}", command, shellName);
            Session.Shell shell = session.startShell();
            shell.setAutoExpand(true);
            OutputStream outputStream = shell.getOutputStream();
            //Interactive command must be ended by new line
            outputStream.write(Strings.toUTF8ByteArray(command + "\n"));
            outputStream.flush();
            InputStream stream = shell.getInputStream();
            //Interactive session must be ended by exit command or sshClient connection drop
            //ttl is there for security reasons when the library don't recognize disconnected channel
            //can happen when aggressive experiments are performed
            int ttl = 0;
            while (sshClient.isConnected() && !shell.isEOF() && shell.isOpen() && !session.isEOF() && ttl < maxSessionDuration) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    log.error("Interactive SSH session watch dog has been interrupted.");
                    break;
                }
                log.debug("Interactive SSH session {} is still active, TTL: {}", shellName, (maxSessionDuration - ttl));
                ttl++;
            }
            log.debug("Interactive SSH session {} has ended. Closing the shell", shellName);
            if (session.isOpen()) {
                session.close();
            }
            if (shell.isOpen()) {
                shell.close();
            }
            log.debug("Interactive shell session ended.");
    }

    public SshCommandResult executeCommand (String command) throws IOException {
            Session session = sshClient.startSession();
            session.allocateDefaultPTY();
            log.debug("Going to execute command: {}", command);
            Command cmd = session.exec(command);
            cmd.join(120, TimeUnit.SECONDS);
            SshCommandResult result = new SshCommandResult(cmd);
            session.close();
            log.debug("Command execution finished with exit code: {}", cmd.getExitStatus());
            return result;
    }

    public void disconnect () throws  IOException{
            if (sshClient.isConnected()) {
                sshClient.disconnect();
            }
    }
}
