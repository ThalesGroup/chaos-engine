package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.scripts.Script;

public interface ShellBasedExperiment<T extends Container> {
    void recycleContainer (T container);

    /**
     * This will run a command against a container and return the output.
     * If it does not return successfully, should throw a RuntimeException.
     *
     * @param container          The container to run the command against
     * @param selfHealingCommand The command to be run
     * @return The output of the command
     */
    String runCommand (T container, String selfHealingCommand);

    /**
     * This will run a script against a container and return the output.
     * If it does not return successfully, should throw a RuntimeException.
     *
     * @param container The container to run the script against
     * @param script    The script to be run
     * @return The output of the script
     */
    String runScript (T container, Script script);
}
