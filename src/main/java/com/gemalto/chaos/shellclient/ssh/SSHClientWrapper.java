package com.gemalto.chaos.shellclient.ssh;

import com.gemalto.chaos.constants.SSHConstants;
import com.gemalto.chaos.shellclient.ShellClient;

public interface SSHClientWrapper extends ShellClient {
    default SSHClientWrapper connect () {
        return connect(SSHConstants.THIRTY_SECONDS_IN_MILLIS);
    }

    SSHClientWrapper connect (int connectionTimeout);

    SSHClientWrapper withSSHCredentials (SSHCredentials sshCredentials);

    SSHClientWrapper withEndpoint (String hostname);

    SSHClientWrapper withEndpoint (String hostname, Integer portNumber);
}
