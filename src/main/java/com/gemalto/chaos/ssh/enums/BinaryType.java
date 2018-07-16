package com.gemalto.chaos.ssh.enums;

public enum BinaryType {
    KILL("kill"),
    TYPE("type"),
    GREP("grep"),
    UNKNOWN("unknown");
    private String binaryType;

    BinaryType (String binaryType) {
        this.binaryType = binaryType;
    }
}
