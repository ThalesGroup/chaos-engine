package com.gemalto.chaos.shellclient;

import org.springframework.core.io.Resource;

import java.io.Closeable;
import java.util.Arrays;

import static com.gemalto.chaos.shellclient.ShellConstants.PARAMETER_DELIMITER;

public interface ShellClient extends Closeable {
    default Boolean checkDependency (String shellCapability) {
        String[] testCommands = new String[]{ "command -v", "which", "type" };
        return Arrays.stream(testCommands).map(command -> command + PARAMETER_DELIMITER + shellCapability)
                     .map(this::runCommand)
                     .map(ShellOutput::getExitCode)
                     .anyMatch(exitCode -> exitCode == 0);
    }

    String runResource (Resource resource);

    ShellOutput runCommand (String command);
}
