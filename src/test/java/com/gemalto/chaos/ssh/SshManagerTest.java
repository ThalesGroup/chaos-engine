package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SshManagerTest {
    private final String host = "host";
    private final int port = 123;
    private final String username = "username";
    private final String passwd = "passwd";
    @Mock
    SSHClient sshClient;

    @Test
    public void connect () {
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(true);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        boolean connected = sshManager.connect(username, passwd);
        try {
            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            verify(sshClient, times(1)).isConnected();
            verify(sshClient, times(1)).isAuthenticated();
            assertTrue(connected);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void connectionLost () {
        when(sshClient.isConnected()).thenReturn(false);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        boolean connected = sshManager.connect(username, passwd);
        try {
            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            assertFalse(connected);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void connectionFailed () {
        try {
            doThrow(new IOException()).when(sshClient).connect(host, port);
            SshManager sshManager = new SshManager(host, String.valueOf(port));
            sshManager.setSshClient(sshClient);
            boolean connected = sshManager.connect(username, passwd);
            verify(sshClient, times(1)).connect(host, port);
            assertFalse(connected);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void authenticationFailed () {
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(false);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        boolean connected = sshManager.connect(username, passwd);
        try {
            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            verify(sshClient, times(1)).isConnected();
            verify(sshClient, times(1)).isAuthenticated();
            assertFalse(connected);
        } catch (Exception e) {
            fail();
        }
    }
}