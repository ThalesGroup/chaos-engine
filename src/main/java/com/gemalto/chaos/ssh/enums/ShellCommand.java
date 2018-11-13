package com.gemalto.chaos.ssh.enums;

public enum ShellCommand {
    SHELLTYPE("echo $0"),
    BINARYEXISTS("type "); //TODO replace type command with which for better compatibility with ubuntu based systems
    private String command;

    ShellCommand (String command) {
        this.command = command;
    }

    @Override
    public String toString () {
        return command;
    }
}
