package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RandomProcessTerminationTest {
    @Mock
    private SshManager sshManager;
    @Mock
    private SshCommandResult resultShellCapability;
    @Mock
    private SshCommandResult resultTypeCapability;
    @Mock
    private SshCommandResult resultGrepCapability;
    @Mock
    private SshCommandResult resultKillCapability;
    @Mock
    private SshCommandResult resultSortCapability;
    @Mock
    private SshCommandResult resultHeadCapability;

    @Before
    public void setUp () throws IOException {
        when(resultShellCapability.getExitStatus()).thenReturn(0);
        when(resultShellCapability.getCommandOutput()).thenReturn("bash");
        when(resultTypeCapability.getExitStatus()).thenReturn(0);
        when(resultTypeCapability.getCommandOutput()).thenReturn("type");
        when(resultGrepCapability.getExitStatus()).thenReturn(0);
        when(resultGrepCapability.getCommandOutput()).thenReturn("grep");
        when(resultKillCapability.getExitStatus()).thenReturn(0);
        when(resultKillCapability.getCommandOutput()).thenReturn("kill");
        when(resultSortCapability.getExitStatus()).thenReturn(0);
        when(resultSortCapability.getCommandOutput()).thenReturn("sort");
        when(resultHeadCapability.getExitStatus()).thenReturn(0);
        when(resultHeadCapability.getCommandOutput()).thenReturn("head");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.GREP)).thenReturn(resultGrepCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.KILL)).thenReturn(resultKillCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.SORT)).thenReturn(resultSortCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.HEAD)).thenReturn(resultHeadCapability);
    }

    @Test
    public void canExperiment () throws IOException {
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertTrue(randomProcessTermination.runExperiment(sshManager));
    }

    @Test
    public void cannotExperiment () throws IOException {
        when(resultShellCapability.getExitStatus()).thenReturn(0);
        when(resultShellCapability.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertFalse(randomProcessTermination.runExperiment(sshManager));
    }

    @Test
    public void cannotRetrieveCapabilities () throws IOException {
        when(resultShellCapability.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertFalse(randomProcessTermination.runExperiment(sshManager));
        randomProcessTermination = new RandomProcessTermination();
        when(resultShellCapability.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        assertFalse(randomProcessTermination.runExperiment(sshManager));
        randomProcessTermination = new RandomProcessTermination();
        when(resultTypeCapability.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
        assertFalse(randomProcessTermination.runExperiment(sshManager));
        randomProcessTermination = new RandomProcessTermination();
        when(resultTypeCapability.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
        assertFalse(randomProcessTermination.runExperiment(sshManager));
    }
}