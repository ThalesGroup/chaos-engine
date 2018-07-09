package com.gemalto.chaos.ssh.enums;

public enum ShellCommand {
    SHELLTYPE("echo $0");
    private String command;

    ShellCommand (String command) {
        this.command = command;
    }

    @Override
    public String toString () {
        return command;
    }
}
