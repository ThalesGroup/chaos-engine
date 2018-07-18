package com.gemalto.chaos.ssh.impl.attacks;

import com.gemalto.chaos.ssh.ShellSessionCapabilityProvider;
import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.BinaryType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RandomProcessTerminationTest {
    @Mock
    private SshManager sshManager;
    @Mock
    private ShellSessionCapabilityProvider capabilitiesProvider;
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
    public void setUp () {
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
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.TYPE.getBinaryName())).thenReturn(resultTypeCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.GREP.getBinaryName())).thenReturn(resultGrepCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.KILL.getBinaryName())).thenReturn(resultKillCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.SORT.getBinaryName())).thenReturn(resultSortCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.HEAD.getBinaryName())).thenReturn(resultHeadCapability);
    }

    @Test
    public void canAttack () {
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertTrue(randomProcessTermination.attack(sshManager));
    }

    @Test
    public void cannotAttack () {
        when(resultShellCapability.getExitStatus()).thenReturn(0);
        when(resultShellCapability.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertFalse(randomProcessTermination.attack(sshManager));
    }

    @Test
    public void cannotRetrieveCapabilities () {
        when(resultShellCapability.getExitStatus()).thenReturn(1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        RandomProcessTermination randomProcessTermination = new RandomProcessTermination();
        assertFalse(randomProcessTermination.attack(sshManager));
        when(resultShellCapability.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        assertFalse(randomProcessTermination.attack(sshManager));
        when(resultTypeCapability.getExitStatus()).thenReturn(-1);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.TYPE.getBinaryName())).thenReturn(resultTypeCapability);
        assertFalse(randomProcessTermination.attack(sshManager));
        when(resultTypeCapability.getExitStatus()).thenReturn(2);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.TYPE.getBinaryName())).thenReturn(resultTypeCapability);
        assertFalse(randomProcessTermination.attack(sshManager));
    }
}