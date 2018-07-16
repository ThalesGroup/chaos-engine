package com.gemalto.chaos.ssh.enums;

public enum BinaryType {
    KILL("kill"),
    TYPE("type"),
    GREP("grep"),
    SORT("sort"),
    UNKNOWN("unknown");
    private String binaryName;

    BinaryType (String binaryName) {
        this.binaryName = binaryName;
    }

    public String getBinaryName () {
        return binaryName;
    }
}
