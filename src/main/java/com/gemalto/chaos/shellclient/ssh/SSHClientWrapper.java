package com.gemalto.chaos.shellclient.ssh;

import com.gemalto.chaos.shellclient.ShellClient;

public interface SSHClientWrapper extends ShellClient {
    SSHClientWrapper connect ();

    SSHClientWrapper withSSHCredentials (SSHCredentials sshCredentials);

    SSHClientWrapper withEndpoint (String hostname);

    SSHClientWrapper withEndpoint (String hostname, Integer portNumber);
}
