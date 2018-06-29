package com.gemalto.chaos.ssh.impl.attacks;

import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshAttack;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ForkBomb extends SshAttack {
    private static final Logger log = LoggerFactory.getLogger(ForkBomb.class);

    public ForkBomb (ArrayList<ShellSessionCapability> actualCapabilities, SshManager sshManager) {
        super(actualCapabilities, sshManager);
        buildRequiredCapabilities();
    }

    @Override
    protected void buildRequiredCapabilities () {
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellType.BASH
                .toString())
                                                                                      .addCapabilityOption(ShellType.ASH
                                                                                              .toString())
                                                                                      .addCapabilityOption(ShellType.SH.toString()));
    }

    @Override
    protected String getAttackCommand () {
        return "sleep 65";
    }
}
