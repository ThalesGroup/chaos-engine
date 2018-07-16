package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ShellSessionCapabilityTest {
    ShellSessionCapability shellCapability;
    ShellSessionCapability binaryCapability;

    @Before
    public void setUp () throws Exception {
        shellCapability = new ShellSessionCapability(ShellCapabilityType.SHELL);
        shellCapability.addCapabilityOption("opt1");
        shellCapability.addCapabilityOption("opt2");
        shellCapability.addCapabilityOption("opt3");
        binaryCapability = new ShellSessionCapability(ShellCapabilityType.BINARY);
        binaryCapability.addCapabilityOption("opt1");
        binaryCapability.addCapabilityOption("opt2");
        binaryCapability.addCapabilityOption("opt3");
    }

    @Test
    public void hasAnOption () {
        ArrayList<String> reqOpts = new ArrayList<>();
        assertEquals(false, shellCapability.hasAnOption(reqOpts));
        reqOpts.add("opt4");
        assertEquals(false, shellCapability.hasAnOption(reqOpts));
        reqOpts.add("opt3");
        assertEquals(true, shellCapability.hasAnOption(reqOpts));
        reqOpts.add("opt1");
        assertEquals(true, shellCapability.hasAnOption(reqOpts));
    }

    @Test
    public void getCapabilityType () {
        assertEquals(ShellCapabilityType.SHELL, shellCapability.getCapabilityType());
        assertEquals(ShellCapabilityType.BINARY, binaryCapability.getCapabilityType());
    }
}