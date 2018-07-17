package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.BinaryType;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShellSessionCapabilityProviderTest {
    @Mock
    SshManager ssh;
    @Mock
    SshCommandResult result;
    ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    ShellSessionCapabilityProvider provider;
    ShellSessionCapability capabilityBash = new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellType.BASH
            .name());
    ShellSessionCapability capabilityBinary = new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.GREP
            .name());

    @Test
    public void sessionHasShellCapability () {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        ArrayList<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBash);
        assertEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    private void initCapabilities (ShellSessionCapability requiredCapability) {
        requiredCapabilities = new ArrayList<>();
        requiredCapabilities.add(requiredCapability);
        provider = new ShellSessionCapabilityProvider(ssh, requiredCapabilities);
    }

    @Test
    public void sessionHasBinaryCapability () {
        initCapabilities(capabilityBinary);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("test string");
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.GREP.name())).thenReturn(result);
        provider.build();
        ArrayList<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBinary);
        assertEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    @Test
    public void sessionShellCapabilityMissing () {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("UNKNOWN");
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        ArrayList<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBash);
        assertTrue(provider.getCapabilities().size() == 1);
        assertNotEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    @Test
    public void sessionBinaryCapabilityMissing () {
        initCapabilities(capabilityBinary);
        when(result.getExitStatus()).thenReturn(1);
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + BinaryType.GREP.name())).thenReturn(result);
        provider.build();
        assertTrue(provider.getCapabilities().size() == 0);
    }

    @Test
    public void commandExecutionHasFailed () {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(-1);
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        assertTrue(provider.getCapabilities().size() == 0);
    }
}