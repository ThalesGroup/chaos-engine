package com.gemalto.chaos.ssh;

import net.schmizz.sshj.connection.channel.direct.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SshCommandResultTest {
    private final String okString = "Success";
    private final String empty = "";
    private final int okExitCode = 0;
    private final int koExitCode = -1;
    private final String executionSuccess = "Command exit status: " + okExitCode + ", command output: " + okString;
    private final String executionFailed = "Command execution failed or execution has been interrupted, exit status: " + koExitCode;
    @Mock
    private Session.Command command;

    @Test
    public void commandOutputExtraction () {
        InputStream stream = new ByteArrayInputStream(okString.getBytes());
        when(command.getExitStatus()).thenReturn(okExitCode);
        when(command.getInputStream()).thenReturn(stream);
        SshCommandResult result = new SshCommandResult(command);
        assertEquals(okString, result.getCommandOutput());
        assertEquals(okExitCode, result.getExitStatus());
    }

    @Test
    public void errorResult () {
        SshCommandResult result = new SshCommandResult(koExitCode);
        assertEquals(empty, result.getCommandOutput());
        assertEquals(koExitCode, result.getExitStatus());
    }

    @Test
    public void resultToString () {
        SshCommandResult result = new SshCommandResult(koExitCode);
        assertEquals(executionFailed, result.toString());
        InputStream stream = new ByteArrayInputStream(okString.getBytes());
        when(command.getExitStatus()).thenReturn(okExitCode);
        when(command.getInputStream()).thenReturn(stream);
        result = new SshCommandResult(command);
        assertEquals(executionSuccess, result.toString());
    }
}