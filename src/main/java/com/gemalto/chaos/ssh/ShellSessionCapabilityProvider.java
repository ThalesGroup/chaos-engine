package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellType;

import java.util.ArrayList;

public class ShellSessionCapabilityProvider {
    private SshManager sshManager;
    private ArrayList<ShellSessionCapability> capabilities = new ArrayList<>();

    public ShellSessionCapabilityProvider (SshManager sshManager) {
        this.sshManager = sshManager;
    }

    public ArrayList<ShellSessionCapability> getCapabilities () {
        return capabilities;
    }

    public void build () {
        capabilities.add(getShellType());
    }

    private ShellSessionCapability getShellType () {
        ShellSessionCapability capability;
        SshCommandResult result = sshManager.executeCommand(ShellCommand.SHELLTYPE.toString());
        if (result.getExitStatus() != -1 && result.getCommandOutput().length() > 0) {
            String commandOutput = result.getCommandOutput();
            commandOutput = commandOutput.toUpperCase().trim();
            for (ShellType type : ShellType.values()) {
                if (type.toString().matches(commandOutput)) {
                    capability = new ShellSessionCapability(ShellCapabilityType.SHELL);
                    capability.addCapabilityOption(type.name());
                    return capability;
                }
            }
        }
        capability = new ShellSessionCapability(ShellCapabilityType.SHELL);
        capability.addCapabilityOption(ShellType.UNKNOWN.name());
        return capability;
    }
}
