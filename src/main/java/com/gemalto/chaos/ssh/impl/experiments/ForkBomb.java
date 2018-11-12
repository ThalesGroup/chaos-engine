package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshExperiment;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;

public class ForkBomb extends SshExperiment {
    public static final String EXPERIMENT_NAME ="Fork Bomb";
    public static final String EXPERIMENT_SCRIPT ="forkBomb.sh";

    public ForkBomb () {
        super(EXPERIMENT_NAME, EXPERIMENT_SCRIPT);
        buildRequiredCapabilities();
    }

    @Override
    protected void buildRequiredCapabilities () {
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.ASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.SH));
    }



}
