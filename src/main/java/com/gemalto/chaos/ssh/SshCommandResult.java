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

    private void processCommandOutput () {
        try {
            InputStream inputStream = command.getInputStream();
            commandOutput = IOUtils.readFully(inputStream).toString();
        } catch (IOException e) {
            log.error("Unable to read command output '{}'", command, e.getMessage());
            exitStatus=-1;
        }
    }

    public SshCommandResult (int exitStatus) {
        this.exitStatus = exitStatus;
    }

    SshCommandResult (String commandOutput, int exitStatus) {
        this.commandOutput = commandOutput;
        this.exitStatus = exitStatus;
    }

    @Override
    public String toString () {
        if (exitStatus == -1) {
            return "Command execution failed or execution has been interrupted, exit status: " + exitStatus;
        }
        return "Command exit status: " + exitStatus + ", command output: " + commandOutput;
    }

    public int getExitStatus () {
        return exitStatus;
    }

    public String getCommandOutput () {
        return commandOutput;
    }
}
