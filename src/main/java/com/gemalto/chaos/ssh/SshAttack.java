package com.gemalto.chaos.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class SshAttack {
    private static final Logger log = LoggerFactory.getLogger(SshAttack.class);
    protected ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    protected ArrayList<ShellSessionCapability> actualCapabilities;
    protected SshManager sshManager;

    public SshAttack (ArrayList<ShellSessionCapability> actualCapabilities, SshManager sshManager) {
        this.actualCapabilities = actualCapabilities;
        this.sshManager = sshManager;
    }

    protected abstract void buildRequiredCapabilities ();

    public boolean attack () {
        if (shellHasRequiredCapabilities()) {
            log.debug("Starting {} attack.", getAttackName());
            sshManager.executeCommandInAsyncShell(getAttackCommand());
            log.debug("Attack {} deployed.", getAttackName());
            return true;
        } else {
            log.debug("Cannot execute SSH attack {}. Current shell session does not have all required capabilities: {}", getAttackName(), requiredCapabilities);
        }
        return false;
    }

    protected abstract String getAttackName ();

    public boolean shellHasRequiredCapabilities () {
        if (requiredCapabilities.isEmpty()) {
            return false;
        }
        for (ShellSessionCapability required : requiredCapabilities) {
            if (!requiredCapabilityMet(required)) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getAttackCommand ();

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
