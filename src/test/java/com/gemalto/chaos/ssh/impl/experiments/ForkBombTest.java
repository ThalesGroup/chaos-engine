package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;

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
    @Mock
    Resource resource;
    @Mock
    ShResourceService shResourceService;

    ForkBomb bomb = new ForkBomb();

    @Before
    public void setUp () throws Exception {
        when(shResourceService.getScriptResource(ForkBomb.EXPERIMENT_SCRIPT)).thenReturn(resource);
        bomb.setSshManager(sshManager).setShResourceService(shResourceService);
    }

    @Test
    public void canExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);


        assertTrue(bomb.runExperiment());
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment());

        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment());
        bomb = new ForkBomb();
        bomb.setSshManager(sshManager).setShResourceService(shResourceService);
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment());
    }

    @Test
    public void parseBinaryName () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertTrue(bomb.runExperiment());
    }

    @Test
    public void cannotExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertFalse(bomb.runExperiment());
    }

    @Test
    public void cannotRetrieveCapabilities () throws IOException {
        when(result.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertFalse(bomb.runExperiment());
        when(result.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        assertFalse(bomb.runExperiment());
    }
}