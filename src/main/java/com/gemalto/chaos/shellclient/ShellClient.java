package com.gemalto.chaos.shellclient;

import org.springframework.core.io.Resource;

import java.io.Closeable;

public interface ShellClient extends Closeable {
    default Boolean checkDependency (String shellCapability) {
        switch (runCommand("which " + shellCapability).getExitCode()) {
            case 0: // Exists as file
            case 1: // Exists as Shell utility
                return Boolean.TRUE;
            default:
                return Boolean.FALSE;
        }
    }

    String runResource (Resource resource);

    ShellOutput runCommand (String command);
}
