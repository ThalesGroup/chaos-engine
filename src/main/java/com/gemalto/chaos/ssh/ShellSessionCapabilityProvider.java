package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    public void build () throws IOException {
        log.debug("Collecting shell session capabilities");
        getAvailableCapabilities();
    }

    private void getAvailableCapabilities () throws IOException {
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

    private void checkShellType () throws IOException {
        ShellSessionCapability capability;
        SshCommandResult result = sshManager.executeCommand(ShellCommand.SHELLTYPE.toString());
        if (result.getExitStatus() == 0 && result.getCommandOutput().length() > 0) {
            String shellName = result.getCommandOutput();
            shellName = shellName.toLowerCase().trim();
            shellName = parseFileNameFromFilePath(shellName);
            for (ShellSessionCapabilityOption type : ShellSessionCapabilityOption.getShellTypes()) {
                if (type.getName().matches(shellName)) {
                    capability = new ShellSessionCapability(ShellCapabilityType.SHELL);
                    capability.addCapabilityOption(type);
                    capabilities.add(capability);
                }
            }
        }
    }

    private void checkBinaryPresent (ArrayList<ShellSessionCapabilityOption> binaryOptions) throws IOException {
        ShellSessionCapability capability = new ShellSessionCapability(ShellCapabilityType.BINARY);
        for (ShellSessionCapabilityOption option : binaryOptions) {
            SshCommandResult result = sshManager.executeCommand(ShellCommand.BINARYEXISTS + option.getName());
            if (result.getExitStatus() == 0 && result.getCommandOutput().length() > 0) {
                capability.addCapabilityOption(option);
            }
        }
        if (!capability.optionsEmpty()) {
            capabilities.add(capability);
        }
    }

    private String parseFileNameFromFilePath (String filepath) {
        return new File(filepath).getAbsoluteFile().getName();
    }
}
