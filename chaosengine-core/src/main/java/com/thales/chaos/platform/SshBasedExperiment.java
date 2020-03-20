/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

import com.thales.chaos.constants.SSHConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.shellclient.ShellClient;
import com.thales.chaos.shellclient.ssh.SSHCredentials;
import com.thales.chaos.shellclient.ssh.impl.ChaosSSHClient;

import java.io.IOException;

public interface SshBasedExperiment<T extends Container> extends ShellBasedExperiment<T> {
    default ShellClient getConnectedShellClient (T container) throws IOException {
        return new ChaosSSHClient().withEndpoint(getEndpoint(container))
                                   .withSSHCredentials(getSshCredentials(container))
                                   .withRunningDirectory(getRunningDirectory())
                                   .connect();
    }

    /**
     * Provide a hostname, and potentially port, for the SSH Client to connect to.
     *
     * @param container The container for which the SSH Endpoint is requested
     * @return Either a string of a hostname, or a string formatted as "%hostname%:%port%",
     * where hostname is a String, and port is an Integer
     */
    String getEndpoint (T container);

    /**
     * Provide a set of SSH Credentials to use when connecting the SSH Client.
     *
     * @param container The container to generate SSH Credentials for
     * @return An SSH Credentials object, with a username, and either a Password Generator, or a private key
     */
    SSHCredentials getSshCredentials (T container);

    default String getRunningDirectory () {
        return SSHConstants.TEMP_DIRECTORY;
    }
}
