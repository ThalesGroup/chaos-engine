package com.gemalto.chaos.ssh.impl.attacks;

import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshAttack;
import com.gemalto.chaos.ssh.enums.BinaryType;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomProcessTermination extends SshAttack {
    private static final Logger log = LoggerFactory.getLogger(RandomProcessTermination.class);

    public RandomProcessTermination () {
        super();
        buildRequiredCapabilities();
    }

    @Override
    protected void buildRequiredCapabilities () {
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellType.BASH
                .toString())
                                                                                      .addCapabilityOption(ShellType.ASH
                                                                                              .toString())
                                                                                      .addCapabilityOption(ShellType.SH.toString()));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.TYPE
                .getBinaryName()));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.GREP
                .getBinaryName()));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.KILL
                .getBinaryName()));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.SORT
                .getBinaryName()));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(BinaryType.HEAD
                .getBinaryName()));
    }

    @Override
    protected String getAttackName () {
        return "Random Process Termination";
    }

    @Override
    protected String getAttackCommand () {
        return "kill $(cd /proc;ls -1 | grep '[0-9]' |sort -R | head -1)";
    }

    @Override
    protected int getSshSessionMaxDuration () {
        return 10;
    }
}
