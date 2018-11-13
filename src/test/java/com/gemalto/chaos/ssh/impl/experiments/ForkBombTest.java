package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ForkBombTest {
    @Mock
    SshManager sshManager;
    @Mock
    SshCommandResult result;
    @Mock
    private SshCommandResult resultTypeCapability;
    @Mock
    Resource resource;
    @Mock
    ShResourceService shResourceService;
    ForkBomb bomb = new ForkBomb();

    @Before
    public void setUp () throws IOException {
        when(shResourceService.getScriptResource(ForkBomb.EXPERIMENT_SCRIPT)).thenReturn(resource);
        bomb.setSshManager(sshManager).setShResourceService(shResourceService);
        when(resultTypeCapability.getExitStatus()).thenReturn(0);
        when(resultTypeCapability.getCommandOutput()).thenReturn("type");
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
    }

    @Test
    public void canExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
        bomb = new ForkBomb();
        bomb.setSshManager(sshManager).setShResourceService(shResourceService);
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
    }

    @Test
    public void parseBinaryName () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
    }

    @Test(expected = ChaosException.class)
    public void cannotExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        bomb.runExperiment();
    }

    @Test
    public void cannotRetrieveCapabilities () throws IOException {
        when(result.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        try {
            bomb.runExperiment();
            fail();
        }catch (ChaosException ex){

        }
        when(result.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        try {
            bomb.runExperiment();
            fail();
        }catch (ChaosException ex){

        }
    }
}