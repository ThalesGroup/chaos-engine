package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class ShellSessionCapabilityProvider {
    private static final Logger log = LoggerFactory.getLogger(ShellSessionCapabilityProvider.class);
    private SshManager sshManager;
    private ArrayList<ShellSessionCapability> capabilities = new ArrayList<>();
    private ArrayList<ShellSessionCapability> requiredCapabilities;

    public ShellSessionCapabilityProvider (SshManager sshManager, ArrayList<ShellSessionCapability> requiredCapabilities) {
        this.sshManager = sshManager;
        this.requiredCapabilities = requiredCapabilities;
    }

    public ArrayList<ShellSessionCapability> getCapabilities () {
        return capabilities;
    }

    public void build () {
        log.debug("Collecting shell session capabilities");
        getAvailableCapabilities();
    }

    private void getAvailableCapabilities () {
        for (ShellSessionCapability requiredCapability : requiredCapabilities) {
            switch (requiredCapability.getCapabilityType()) {
                case SHELL:
                    checkShellType();
                    break;
                case BINARY:
                    checkBinaryPresent(requiredCapability.getCapabilityOptions());
                    break;
            }
        }
    }

    private void checkShellType () {
        ShellSessionCapability capability;
        SshCommandResult result = sshManager.executeCommand(ShellCommand.SHELLTYPE.toString());
        if (result.getExitStatus() == 0 && result.getCommandOutput().length() > 0) {
            String shellName = result.getCommandOutput();
            shellName = shellName.toUpperCase().trim();
            shellName = parseFileNameFromFilePath(shellName);
            for (ShellType type : ShellType.values()) {
                if (type.toString().matches(shellName)) {
                    capability = new ShellSessionCapability(ShellCapabilityType.SHELL);
                    capability.addCapabilityOption(type.name());
                    capabilities.add(capability);
                }
            }
        } else {
            capability = new ShellSessionCapability(ShellCapabilityType.SHELL);
            capability.addCapabilityOption(ShellType.UNKNOWN.name());
            capabilities.add(capability);
        }
    }

    private void checkBinaryPresent (ArrayList<String> binaryOptions) {
        for (String option : binaryOptions) {
            SshCommandResult result = sshManager.executeCommand(ShellCommand.BINARYEXISTS + option);
            if (result.getExitStatus() == 0 && result.getCommandOutput().length() > 0) {
                //String shellName = result.getCommandOutput();
                ShellSessionCapability capability = new ShellSessionCapability(ShellCapabilityType.BINARY);
                capability.addCapabilityOption(option);
                capabilities.add(capability);
            }
        }
    }

    private String parseFileNameFromFilePath (String filepath) {
        return new File(filepath).getAbsoluteFile().getName();
    }
}
