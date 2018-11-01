package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ForkBombTest {
    @Mock
    SshManager sshManager;
    @Mock
    SshCommandResult result;

    @Test
    public void canExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertTrue(bomb.runExperiment(sshManager));
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment(sshManager));
        bomb = new ForkBomb();
        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment(sshManager));
        bomb = new ForkBomb();
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment(sshManager));
    }

    @Test
    public void parseBinaryName () throws IOException {
        ForkBomb bomb = new ForkBomb();
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment(sshManager));
    }

    @Test
    public void cannotExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertFalse(bomb.runExperiment(sshManager));
    }

    @Test
    public void cannotRetrieveCapabilities () throws IOException {
        when(result.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertFalse(bomb.runExperiment(sshManager));
        bomb = new ForkBomb();
        when(result.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertFalse(bomb.runExperiment(sshManager));
    }
}