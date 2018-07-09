package com.gemalto.chaos.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class SshAttack {
    private static final Logger log = LoggerFactory.getLogger(SshAttack.class);
    protected ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    protected ArrayList<ShellSessionCapability> actualCapabilities;
    protected SshManager sshManager;

    public SshAttack (SshManager sshManager) {
        this.sshManager = sshManager;
        ShellSessionCapabilityProvider cappProvider = sshManager.getShellCapabilities();
        actualCapabilities = cappProvider.getCapabilities();
    }

    protected abstract void buildRequiredCapabilities ();

    public boolean attack () {
        if (shellHasRequiredCapabilities()) {
            log.debug("Starting {} attack.", getAttackName());
            sshManager.executeCommandInInteractiveShell(getAttackCommand(), getAttackName(), getSshSessionMaxDuration());
            log.debug("Attack {} deployed.", getAttackName());
            return true;
        } else {
            log.debug("Cannot execute SSH attack {}. Current shell session does not have all required capabilities: {}", getAttackName(), requiredCapabilities);
        }
        return false;
    }

    public boolean shellHasRequiredCapabilities () {
        log.debug("Checking SSH attack required capabilities");
        for (ShellSessionCapability required : requiredCapabilities) {
            if (!requiredCapabilityMet(required)) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getAttackName ();

    protected abstract String getAttackCommand ();

    protected abstract int getSshSessionMaxDuration ();

    private boolean requiredCapabilityMet (ShellSessionCapability required) {
        for (ShellSessionCapability actual : actualCapabilities) {
            if (actual.getCapabilityType() == required.getCapabilityType()) {
                if (actual.hasAnOption(required.getCapabilityOptions())) {
                    return true;
                }
            }
        }
        return false;
    }
}
