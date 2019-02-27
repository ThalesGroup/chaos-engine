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

    String getEndpoint (T container);

    SSHCredentials getSshCredentials (T container);
}
