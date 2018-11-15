package com.gemalto.chaos.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SshManagerTest {
    private final String host = UUID.randomUUID().toString();
    private final int port = 123;
    private final String username = UUID.randomUUID().toString();
    private final String passwd = UUID.randomUUID().toString();
    private final String script = UUID.randomUUID().toString();
    private final String shellname = UUID.randomUUID().toString();
    @Mock
    SSHClient sshClient;
    @Mock
    Session session;
    @Mock
    Session.Shell shell;
    @Mock
    OutputStream outputStream;
    @Mock
    Session.Command command;

    @Test
    public void connect () {
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
            fail("No exception should be thrown while connecting to a valid target");
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
            fail("No exception should be thrown when connection interrupted");
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
            fail("No exception should be thrown when authentication failed");
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
            fail("No exception should be thrown when command executed successfully");
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

    @Test
    public void uploadFile () throws IOException {
        File fileToTransfer = Mockito.mock(File.class);
        SCPFileTransfer scpFileTransfer = Mockito.mock(SCPFileTransfer.class);
        when(sshClient.newSCPFileTransfer()).thenReturn(scpFileTransfer);
        String destinationPath = "/tmp";
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        sshManager.uploadFile(fileToTransfer,destinationPath);
        verify(scpFileTransfer, times(1)).upload(new FileSystemFile(fileToTransfer),destinationPath);
    }

    @Test
    public void uploadResource () throws IOException {
        Resource resource = Mockito.mock(Resource.class);
        SCPFileTransfer scpFileTransfer = Mockito.mock(SCPFileTransfer.class);
        when(sshClient.newSCPFileTransfer()).thenReturn(scpFileTransfer);
        when(resource.getFilename()).thenReturn("");
        String destinationPath = "/tmp";
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        sshManager.uploadResource(resource,destinationPath,true);
        verify(scpFileTransfer, times(1)).upload(new JarResourceFile(resource,true),destinationPath);
    }

    @Test
    public void commandExecutionInShell () throws IOException {
        when(sshClient.startSession()).thenReturn(session);
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(true);
        when(session.startShell()).thenReturn(shell);
        when(shell.getOutputStream()).thenReturn(outputStream);
        when(session.isOpen()).thenReturn(true);
        when(shell.isOpen()).thenReturn(true);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        sshManager.connect(username, passwd);
        sshManager.executeCommandInShell(script, shellname);
        verify(session, times(1)).allocateDefaultPTY();
        verify(session, times(1)).startShell();
        verify(shell, times(1)).close();
        verify(session, times(1)).close();
    }

    @Test
    public void disconnect () throws IOException {
        when(sshClient.startSession()).thenReturn(session);
        when(sshClient.isConnected()).thenReturn(true);
        when(sshClient.isAuthenticated()).thenReturn(true);
        when(session.startShell()).thenReturn(shell);
        when(shell.getOutputStream()).thenReturn(outputStream);
        when(session.isOpen()).thenReturn(true);
        when(shell.isOpen()).thenReturn(true);
        SshManager sshManager = new SshManager(host, String.valueOf(port));
        sshManager.setSshClient(sshClient);
        sshManager.connect(username, passwd);
        sshManager.executeCommandInShell(script, shellname);
        doThrow(IOException.class).when(sshClient).disconnect();
        sshManager.disconnect();
        verify(sshClient, times(1)).disconnect();
    }
}