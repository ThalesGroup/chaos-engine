package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SshManagerTest {
    private final String host = "host";
    private final int port = 123;
    private final String username = "username";
    private final String passwd = "passwd";
    private final String script = "command";
    @Mock
    SSHClient sshClient;
    @Mock
    Session session;
    @Mock
    Session.Command command;

    @Test
    public void connect ()  {
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(true);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        try {
        boolean connected = sshManager.connect(username, passwd);
            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            verify(sshClient, times(1)).isConnected();
            verify(sshClient, times(1)).isAuthenticated();
            assertTrue(connected);
            sshManager.disconnect();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void connectionLost () {
        when(sshClient.isConnected()).thenReturn(false);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        try {
        boolean connected = sshManager.connect(username, passwd);

            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            assertFalse(connected);
            sshManager.disconnect();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = IOException.class)
    public void connectionFailed () throws IOException {
            doThrow(new IOException()).when(sshClient).connect(host, port);
            SshManager sshManager = new SshManager(host, String.valueOf(port));
            sshManager.setSshClient(sshClient);
            boolean connected = sshManager.connect(username, passwd);
            verify(sshClient, times(1)).connect(host, port);
            assertFalse(connected);
    }

    @Test
    public void authenticationFailed () {
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(false);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        try {
        boolean connected = sshManager.connect(username, passwd);

            verify(sshClient, times(1)).connect(host, port);
            verify(sshClient, times(1)).authPassword(anyString(), anyString());
            verify(sshClient, times(1)).isConnected();
            verify(sshClient, times(1)).isAuthenticated();
            assertFalse(connected);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void executeCommand () {
        String okString = "Success";
        int okExitCode = 0;
        SshCommandResult expectedResult = new SshCommandResult(okString, okExitCode);
        try {
            when(sshClient.startSession()).thenReturn(session);
            when(sshClient.isConnected()).thenReturn(true);
            when(sshClient.isAuthenticated()).thenReturn(true);
            InputStream stream = new ByteArrayInputStream(okString.getBytes());
            when(command.getExitStatus()).thenReturn(okExitCode);
            when(command.getInputStream()).thenReturn(stream);
            when(session.exec(script)).thenReturn(command);
            SshManager sshManager = new SshManager(host, String.valueOf(port));
            sshManager.setSshClient(sshClient);
            sshManager.connect(username, passwd);
            assertEquals(expectedResult.toString(), sshManager.executeCommand(script).toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = IOException.class)
    public void executeCommandFailed () throws IOException {
        String koString = "Fail";
        int koExitCode = -1;
        SshCommandResult expectedResult = new SshCommandResult(koString, koExitCode);
            when(sshClient.startSession()).thenReturn(session);
            when(sshClient.isConnected()).thenReturn(true);
            when(sshClient.isAuthenticated()).thenReturn(true);
            doThrow(new ConnectionException(koString)).when(session).exec(script);
            SshManager sshManager = new SshManager(host, String.valueOf(port));
            sshManager.setSshClient(sshClient);
            sshManager.connect(username, passwd);
            assertEquals(expectedResult.toString(), sshManager.executeCommand(script).toString());
    }
}