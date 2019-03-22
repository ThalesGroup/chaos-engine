package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.scripts.Script;
import com.gemalto.chaos.shellclient.ShellClient;
import com.gemalto.chaos.shellclient.ShellOutput;

import java.io.IOException;

public interface ShellBasedExperiment<T extends Container> {
    /**
     * Recycle a container to give it a new life. Do something with it so the
     * platform will get rid of it, and create new container(s) in its place.
     *
     * @param container The container to be "recycled" by the platform.
     */
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

    /**
     * Create a Shell Client that can access the shell in the container.
     *
     * @param container The container to connect into.
     * @return A Shell Client that is used by the runCommand and runScript methods.
     */
    ShellClient getConnectedShellClient (T container) throws IOException;

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
