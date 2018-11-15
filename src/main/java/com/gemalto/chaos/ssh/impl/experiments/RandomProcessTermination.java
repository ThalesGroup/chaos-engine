package com.gemalto.chaos.ssh.impl.experiments;

import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshExperiment;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;

public class RandomProcessTermination extends SshExperiment {
    public static final String EXPERIMENT_NAME ="Random Process Termination";
    public static final String EXPERIMENT_SCRIPT ="terminateProcess.sh";

    public RandomProcessTermination () {
        super(EXPERIMENT_NAME, EXPERIMENT_SCRIPT);
        buildRequiredCapabilities();
    }

    @Override
    protected void buildRequiredCapabilities () {
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.GREP));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.KILL));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.SORT));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.HEAD));
    }


}
