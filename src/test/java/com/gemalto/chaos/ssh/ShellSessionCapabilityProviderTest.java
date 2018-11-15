package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShellSessionCapabilityProviderTest {
    @Mock
    SshManager ssh;
    @Mock
    SshCommandResult result;
    List<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    ShellSessionCapabilityProvider provider;
    ShellSessionCapability capabilityBash = new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH);
    ShellSessionCapability capabilityBinary = new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.GREP)
                                                                                                    .addCapabilityOption(ShellSessionCapabilityOption.SORT);

    @Test
    public void sessionHasShellCapability () throws IOException {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBash);
        assertEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    private void initCapabilities (ShellSessionCapability requiredCapability) {
        requiredCapabilities = new ArrayList<>();
        requiredCapabilities.add(requiredCapability);
        provider = new ShellSessionCapabilityProvider(ssh, requiredCapabilities);
    }

    @Test
    public void sessionHasBinaryCapability () throws IOException {
        initCapabilities(capabilityBinary);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("test string");
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.GREP.toString())).thenReturn(result);
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.SORT.toString())).thenReturn(result);
        provider.build();
        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBinary);
        assertEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    @Test
    public void sessionHasComplexCapabilityRequirements () throws IOException {
        List<ShellSessionCapability> complexRequiredCapabilities = new ArrayList<>();
        complexRequiredCapabilities.add(capabilityBash);
        complexRequiredCapabilities.add(capabilityBinary);
        ShellSessionCapabilityProvider provider = new ShellSessionCapabilityProvider(ssh, complexRequiredCapabilities);
        initCapabilities(capabilityBinary);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("test string");
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.GREP.toString())).thenReturn(result);
        when(ssh.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.SORT.toString())).thenReturn(result);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBash);
        expectedCapabilities.add(capabilityBinary);
        assertEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    @Test
    public void sessionShellCapabilityMissing () throws IOException {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("UNKNOWN");
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(capabilityBash);
        assertTrue(provider.getCapabilities().size() == 0);
        assertNotEquals(expectedCapabilities.toString(), provider.getCapabilities().toString());
    }

    @Test
    public void sessionBinaryCapabilityMissing () throws IOException {
        initCapabilities(capabilityBinary);
        when(result.getExitStatus()).thenReturn(1);
        when(ssh.executeCommand(any(String.class))).thenReturn(result);
        provider.build();
        assertTrue(provider.getCapabilities().size() == 0);
    }

    @Test
    public void commandExecutionHasFailed () throws IOException {
        initCapabilities(capabilityBash);
        when(result.getExitStatus()).thenReturn(-1);
        when(ssh.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        provider.build();
        assertTrue(provider.getCapabilities().size() == 0);
    }
}