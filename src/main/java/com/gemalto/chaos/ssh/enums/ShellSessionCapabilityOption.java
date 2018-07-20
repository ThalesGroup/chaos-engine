package com.gemalto.chaos.ssh.enums;

public enum ShellSessionCapabilityOption {
    BASH("bash", ShellCapabilityType.SHELL),
    ASH("ash", ShellCapabilityType.SHELL),
    SH("sh", ShellCapabilityType.SHELL),
    KILL("kill", ShellCapabilityType.BINARY),
    TYPE("type", ShellCapabilityType.BINARY),
    GREP("grep", ShellCapabilityType.BINARY),
    SORT("sort", ShellCapabilityType.BINARY),
    HEAD("head", ShellCapabilityType.BINARY);
    private String name;
    private ShellCapabilityType type;

    ShellSessionCapabilityOption (String name, ShellCapabilityType type) {
        this.name = name;
        this.type = type;
    }

    public String getName () {
        return name;
    }

    public ShellCapabilityType getType () {
        return type;
    }
}
