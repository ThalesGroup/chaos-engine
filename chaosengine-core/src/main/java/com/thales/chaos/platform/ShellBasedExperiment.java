/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.platform;

import com.thales.chaos.container.Container;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.shellclient.ShellClient;
import com.thales.chaos.shellclient.ShellOutput;

import java.io.IOException;

import static com.thales.chaos.exception.enums.ChaosErrorCode.*;

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
     * @param container The container to run the command against
     * @param command   The command to be run
     * @return The output of the command
     */
    default ShellOutput runCommand (T container, String command) {
        try (ShellClient shellClient = getConnectedShellClient(container)) {
            return shellClient.runCommand(command);
        } catch (IOException e) {
            throw new ChaosException(SHELL_CLIENT_COMMAND_ERROR, e);
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
                throw new ChaosException(SHELL_CLIENT_DEPENDENCY_ERROR);
            }
            return shellClient.runResource(script.getResource());
        } catch (IOException e) {
            throw new ChaosException(SHELL_CLIENT_TRANSFER_ERROR, e);
        }
    }
}
