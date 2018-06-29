package com.gemalto.chaos.ssh;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SshCommandResult {
    private static final Logger log = LoggerFactory.getLogger(SshCommandResult.class);
    private int exitStatus;
    private Session.Command command;
    private String commandOutput = "";

    public SshCommandResult (Session.Command command) {
        this.command = command;
        this.exitStatus = command.getExitStatus();
        processCommandOutput();
    }

    @Override
    public String toString () {
        return "Command exit status: " + command.getExitStatus() + ", command output: " + commandOutput;
    }

    private void processCommandOutput () {
        try {
            InputStream inputStream = command.getInputStream();
            commandOutput = IOUtils.readFully(inputStream).toString();
        } catch (IOException e) {
            log.error("Unable to read command output '{}' output,  problem: {}", command, e.getMessage());
        }
    }

    public SshCommandResult (Session.Command command, int exitStatus) {
        this.command = command;
        this.exitStatus = exitStatus;
    }

    public int getExitStatus () {
        return exitStatus;
    }

    public Session.Command getCommand () {
        return command;
    }

    public String getCommandOutput () {
        return commandOutput;
    }
}
