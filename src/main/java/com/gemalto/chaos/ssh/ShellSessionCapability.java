package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;

import java.util.ArrayList;
import java.util.Arrays;

public class ShellSessionCapability {
    private ShellCapabilityType capabilityType;
    private ArrayList<ShellSessionCapabilityOption> capabilityOptions = new ArrayList<>();

    public ShellSessionCapability (ShellCapabilityType capabilityType) {
        this.capabilityType = capabilityType;
    }

    public ShellSessionCapability addCapabilityOption (ShellSessionCapabilityOption option) {
        capabilityOptions.add(option);
        return this;
    }

    public boolean optionsEmpty () {
        return capabilityOptions.isEmpty();
    }

    public boolean hasAnOption (ArrayList<ShellSessionCapabilityOption> requiredOptions) {
        for (ShellSessionCapabilityOption option : requiredOptions) {
            for (ShellSessionCapabilityOption allowed : capabilityOptions) {
                if (option == allowed) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals (Object obj) {
        ShellSessionCapability capability = (ShellSessionCapability) obj;
        return this.getCapabilityType() == capability.getCapabilityType() && this.getCapabilityOptions()
                                                                                 .size() == capability.getCapabilityOptions()
                                                                                                      .size() && Arrays.equals(this
                .getCapabilityOptions()
                .toArray(), capability.getCapabilityOptions().toArray());
    }

    @Override
    public String toString () {
        return "Type: " + capabilityType + "; Options: " + capabilityOptions;
    }

    public ShellCapabilityType getCapabilityType () {
        return capabilityType;
    }

    public ArrayList<ShellSessionCapabilityOption> getCapabilityOptions () {
        return capabilityOptions;
    }
}
