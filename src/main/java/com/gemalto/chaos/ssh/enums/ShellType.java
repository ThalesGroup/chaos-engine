package com.gemalto.chaos.ssh.enums;

public enum ShellType {
    BASH("bash"),
    ASH("ash"),
    SH("sh"),
    UNKNOWN("unknown");
    private String shellName;

    ShellType (String shellName) {
        this.shellName = shellName;
    }
}
