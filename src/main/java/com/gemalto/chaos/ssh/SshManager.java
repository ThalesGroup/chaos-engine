package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
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

    public void uploadFile(File file,String destinationPath) throws IOException {
        sshClient.newSCPFileTransfer().upload(new FileSystemFile(file), destinationPath);

    }

    public void uploadResource(Resource resource,String destinationPath,boolean isExecutable) throws IOException {
        sshClient.newSCPFileTransfer().upload(new JarResourceFile(resource,isExecutable), destinationPath);
    }

    public boolean connect (String userName, String password) throws IOException {
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

    public void executeCommandInShell (String command, String shellName) throws IOException {
        Session session = sshClient.startSession();
        session.allocateDefaultPTY();
        log.debug("Running command \'{}\' in shell session {}", command, shellName);
        Session.Shell shell = session.startShell();
        shell.setAutoExpand(true);
        OutputStream outputStream = shell.getOutputStream();
        //Interactive command must be ended by new line
        outputStream.write(Strings.toUTF8ByteArray(command + "\n"));
        outputStream.flush();
        outputStream.close();
        log.debug("Shell session {} has ended.", shellName);
        if (shell.isOpen()) {
            shell.close();
        }
        if (session.isOpen()) {
            session.close();
        }

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

    public void disconnect () throws IOException {
        if (sshClient.isConnected()) {
            sshClient.disconnect();
        }
    }
}
