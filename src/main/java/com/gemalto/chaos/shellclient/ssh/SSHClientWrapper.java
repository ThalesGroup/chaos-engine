package com.gemalto.chaos.shellclient.ssh;

import com.gemalto.chaos.shellclient.ShellClient;

import java.io.Closeable;

public interface SSHClientWrapper extends ShellClient, Closeable {
    SSHClientWrapper connect ();

    SSHClientWrapper withSSHCredentials (SSHCredentials sshCredentials);

    SSHClientWrapper withEndpoint (String hostname);

    SSHClientWrapper withEndpoint (String hostname, Integer portNumber);
}
