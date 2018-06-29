package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;

import java.util.ArrayList;

public class ShellSessionCapability {
    private ShellCapabilityType capabilityType;
    private ArrayList<String> capabilityOptions = new ArrayList<>();

    public ShellSessionCapability (ShellCapabilityType capabilityType) {
        this.capabilityType = capabilityType;
    }

    public ShellSessionCapability addCapabilityOption (String option) {
        capabilityOptions.add(option);
        return this;
    }

    public ArrayList<String> getCapabilityOptions () {
        return capabilityOptions;
    }

    public boolean hasAnOption (ArrayList<String> requiredOptions) {
        for (String option : requiredOptions) {
            for (String allowed : capabilityOptions) {
                if (option.matches(allowed)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString () {
        return "Type: " + capabilityType + "; Options: " + capabilityOptions;
    }

    public ShellCapabilityType getCapabilityType () {
        return capabilityType;
    }
}
