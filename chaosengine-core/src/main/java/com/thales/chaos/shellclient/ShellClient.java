package com.thales.chaos.shellclient;

import org.springframework.core.io.Resource;

import java.io.Closeable;

import static com.thales.chaos.shellclient.ShellConstants.DEPENDENCY_TEST_COMMANDS;
import static com.thales.chaos.shellclient.ShellConstants.PARAMETER_DELIMITER;

public interface ShellClient extends Closeable {
    default Boolean checkDependency (String shellCapability) {
        return DEPENDENCY_TEST_COMMANDS.stream()
                                       .map(command -> command + PARAMETER_DELIMITER + shellCapability)
                                       .map(this::runCommand)
                                       .map(ShellOutput::getExitCode)
                                       .anyMatch(exitCode -> exitCode == 0);
    }

    String runResource (Resource resource);

    ShellOutput runCommand (String command);
}
