package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.shellclient.ShellClient;
import com.gemalto.chaos.shellclient.ssh.SSHCredentials;
import com.gemalto.chaos.shellclient.ssh.impl.ChaosSSHClient;

public interface SshBasedExperiment<T extends Container> extends ShellBasedExperiment<T> {
    default ShellClient getConnectedShellClient (T container) {
        return new ChaosSSHClient().withEndpoint(getEndpoint(container))
                                   .withSSHCredentials(getSshCredentials(container))
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
}
