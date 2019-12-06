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
