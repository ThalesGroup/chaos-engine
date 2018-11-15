package com.gemalto.chaos.ssh.enums;

import java.util.ArrayList;
import java.util.List;

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

    public static List<ShellSessionCapabilityOption> getShellTypes () {
        List<ShellSessionCapabilityOption> shellTypes = new ArrayList<>();
        for (ShellSessionCapabilityOption option : ShellSessionCapabilityOption.values()) {
            if (option.type == ShellCapabilityType.SHELL) {
                shellTypes.add(option);
            }
        }
        return shellTypes;
    }

    public ShellCapabilityType getType () {
        return type;
    }

    @Override
    public String toString () {
        return getName();
    }

    public String getName () {
        return name;
    }
}
