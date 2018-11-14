package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.impl.experiments.ForkBomb;
import com.gemalto.chaos.ssh.impl.experiments.RandomProcessTermination;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
@RunWith(MockitoJUnitRunner.class)
public class SshExperimentTest {
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

    class GenericSshExperiment extends SshExperiment{
        public GenericSshExperiment (String experimentName, String experimentScript) {
            super(experimentName, experimentScript);
        }

        @Override
        protected void buildRequiredCapabilities () {
        }
    }
    private static final String experimentName = "Generic SSH experiment";
    private static final String experimentScript = UUID.randomUUID().toString();
    private GenericSshExperiment genericSshExperiment = new GenericSshExperiment(experimentName,experimentScript);
    @Before
    public void setUp () throws Exception {
        when(shResourceService.getScriptResource(experimentScript)).thenReturn(resource);
        genericSshExperiment.setSshManager(sshManager).setShResourceService(shResourceService);
        when(resultTypeCapability.getExitStatus()).thenReturn(0);
        when(resultTypeCapability.getCommandOutput()).thenReturn("type");
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
    }

    @Test
    public void canExperiment() throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        genericSshExperiment=new GenericSshExperiment(experimentName,experimentScript);
        genericSshExperiment.setSshManager(sshManager).setShResourceService(shResourceService);
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        verify(sshManager, times(4)).disconnect();
    }

    @Test
    public void parseBinaryName () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
    }

    @Test(expected = ChaosException.class)
    public void cannotExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
    }
}