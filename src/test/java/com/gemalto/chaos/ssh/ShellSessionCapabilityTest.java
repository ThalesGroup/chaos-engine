package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ShellSessionCapabilityTest {
    ShellSessionCapability shellCapability;
    ShellSessionCapability binaryCapability;

    @Before
    public void setUp () throws Exception {
        shellCapability = new ShellSessionCapability(ShellCapabilityType.SHELL);
        shellCapability.addCapabilityOption(ShellSessionCapabilityOption.ASH);
        shellCapability.addCapabilityOption(ShellSessionCapabilityOption.BASH);
        binaryCapability = new ShellSessionCapability(ShellCapabilityType.BINARY);
        binaryCapability.addCapabilityOption(ShellSessionCapabilityOption.GREP);
        binaryCapability.addCapabilityOption(ShellSessionCapabilityOption.SORT);
        binaryCapability.addCapabilityOption(ShellSessionCapabilityOption.KILL);
    }

    @Test
    public void hasAnOption () {
        List<ShellSessionCapabilityOption> reqOpts = new ArrayList<>();
        assertEquals(false, shellCapability.hasAnOption(reqOpts));
        reqOpts.add(ShellSessionCapabilityOption.SH);
        assertEquals(false, shellCapability.hasAnOption(reqOpts));
        reqOpts.add(ShellSessionCapabilityOption.BASH);
        assertEquals(true, shellCapability.hasAnOption(reqOpts));
        reqOpts.add(ShellSessionCapabilityOption.ASH);
        assertEquals(true, shellCapability.hasAnOption(reqOpts));
    }

    @Test
    public void getCapabilityType () {
        assertEquals(ShellCapabilityType.SHELL, shellCapability.getCapabilityType());
        assertEquals(ShellCapabilityType.BINARY, binaryCapability.getCapabilityType());
    }
}