package com.gemalto.chaos.ssh.impl.attacks;

import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void canAttack () {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertTrue(bomb.attack(sshManager));
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.attack(sshManager));
        bomb = new ForkBomb();
        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.attack(sshManager));
        bomb = new ForkBomb();
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.attack(sshManager));
    }

    @Test
    public void parseBinaryName () {
        ForkBomb bomb = new ForkBomb();
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.attack(sshManager));
    }

    @Test
    public void cannotAttack () {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertFalse(bomb.attack(sshManager));
    }

    @Test
    public void cannotRetrieveCapabilities () {
        when(result.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        ForkBomb bomb = new ForkBomb();
        assertFalse(bomb.attack(sshManager));
        bomb = new ForkBomb();
        when(result.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertFalse(bomb.attack(sshManager));
    }
}