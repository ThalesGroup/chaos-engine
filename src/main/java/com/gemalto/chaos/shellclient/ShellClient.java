package com.gemalto.chaos.shellclient;

import org.springframework.core.io.Resource;

import java.io.Closeable;

public interface ShellClient extends Closeable {
    default Boolean checkDependency (String shellCapability) {
        return runCommand("which " + shellCapability).getExitCode() == 0;
    }

    String runResource (Resource resource);

    ShellOutput runCommand (String command);
}
