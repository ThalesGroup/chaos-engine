package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellType;

public class ShellSessionCapabilities {
    private SshManager sshManager;

    public ShellSessionCapabilities (SshManager sshManager) {
        this.sshManager = sshManager;
    }

    public ShellType getShellType () {
        SshCommandResult result = sshManager.executeCommand(ShellCommand.SHELLTYPE.toString());
        if (result.getExitStatus() != -1 && result.getCommandOutput().length() > 0) {
            String commandOutput = result.getCommandOutput();
            commandOutput = commandOutput.toUpperCase().trim();
            for (ShellType type : ShellType.values()) {
                if (type.toString().matches(commandOutput)) {
                    return type;
                }
            }
        }
        return ShellType.UNKNOWN;
    }
}
