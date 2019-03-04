package com.gemalto.chaos.platform;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.scripts.Script;
import com.gemalto.chaos.shellclient.ShellClient;
import com.gemalto.chaos.shellclient.ShellOutput;

import java.io.IOException;

public interface ShellBasedExperiment<T extends Container> {
    void recycleContainer (T container);

    /**
     * This will run a command against a container and return the output.
     * If it does not return successfully, should throw a RuntimeException.
     *
     * @param container          The container to run the command against
     * @param command The command to be run
     * @return The output of the command
     */
    default ShellOutput runCommand (T container, String command) {
        try (ShellClient shellClient = getConnectedShellClient(container)) {
            return shellClient.runCommand(command);
        } catch (IOException e) {
            throw new ChaosException(e);
        }
    }

    ShellClient getConnectedShellClient (T container);

    /**
     * This will run a script against a container and return the output.
     * If it does not return successfully, should throw a RuntimeException.
     *
     * @param container The container to run the script against
     * @param script    The script to be run
     * @return The output of the script
     */
    default String runScript (T container, Script script) {
        try (ShellClient shellClient = getConnectedShellClient(container)) {
            if (script.getDependencies()
                      .stream()
                      .map(s -> container.getShellCapabilities().computeIfAbsent(s, shellClient::checkDependency))
                      .anyMatch(Boolean.FALSE::equals)) {
                throw new ChaosException("Found that some dependencies were missing while initializing script");
            }
            return shellClient.runResource(script.getResource());
        } catch (IOException e) {
            throw new ChaosException(e);
        }
    }

}
