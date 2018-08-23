package com.gemalto.chaos.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class SshAttack {
    private static final Logger log = LoggerFactory.getLogger(SshAttack.class);
    protected ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    protected ArrayList<ShellSessionCapability> detectedCapabilities;
    protected SshManager sshManager;

    protected abstract void buildRequiredCapabilities ();

    public boolean attack (SshManager sshManager) {
        this.sshManager = sshManager;
        if (detectedCapabilities == null) {
            collectAvailableCapabilities();
        } else {
            if (availableCapabilitiesOutdated()) {
                updateAvailableCapabilities();
            } else {
                log.debug("Skipping shell session capability collection");
            }
        }
        if (shellHasRequiredCapabilities()) {
            log.debug("Starting {} attack.", getAttackName());
            sshManager.executeCommandInInteractiveShell(getAttackCommand(), getAttackName(), getSshSessionMaxDuration());
            log.debug("Attack {} deployed.", getAttackName());
            return true;
        } else {
            log.warn("Cannot execute SSH attack {}. Current shell session does not have all required capabilities: {}, actual capabilities: {}", getAttackName(), requiredCapabilities, detectedCapabilities);
        }
        return false;
    }

    private void collectAvailableCapabilities () {
        ShellSessionCapabilityProvider capProvider = new ShellSessionCapabilityProvider(sshManager, requiredCapabilities);
        capProvider.build();
        detectedCapabilities = capProvider.getCapabilities();
    }

    private void updateAvailableCapabilities () {
        log.debug("Updating shell capabilities");
        ArrayList<ShellSessionCapability> additionalRequiredCapabilities = new ArrayList<>();
        ArrayList<ShellSessionCapability> additionalDetectedCapabilities;
        for (ShellSessionCapability requiredCap : requiredCapabilities) {
            if (!shellHasCapability(requiredCap)) {
                additionalRequiredCapabilities.add(requiredCap);
            }
        }
        ShellSessionCapabilityProvider capProvider = new ShellSessionCapabilityProvider(sshManager, additionalRequiredCapabilities);
        capProvider.build();
        additionalDetectedCapabilities = capProvider.getCapabilities();
        for (ShellSessionCapability additionalDetectedCapability : additionalDetectedCapabilities) {
            detectedCapabilities.add(additionalDetectedCapability);
        }
    }

    private boolean shellHasCapability (ShellSessionCapability requiredCap) {
        for (ShellSessionCapability actualCap : detectedCapabilities) {
            if (actualCap.getCapabilityType() == requiredCap.getCapabilityType() && actualCap.hasAnOption(requiredCap.getCapabilityOptions())) {
                return true;
            }
        }
        return false;
    }

    private boolean availableCapabilitiesOutdated () {
        for (ShellSessionCapability requiredCap : requiredCapabilities) {
            if (!shellHasCapability(requiredCap)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<ShellSessionCapability> getShellSessionCapabilities () {
        return detectedCapabilities;
    }

    public void setShellSessionCapabilities (ArrayList<ShellSessionCapability> detectedCapabilities) {
        this.detectedCapabilities = detectedCapabilities;
    }

    public boolean shellHasRequiredCapabilities () {
        log.debug("Checking SSH attack required capabilities");
        for (ShellSessionCapability required : requiredCapabilities) {
            if (!requiredCapabilityMet(required)) {
                log.debug("SSH Session does not have capability: {}", required);
                return false;
            }
        }
        return true;
    }


    protected abstract String getAttackName ();

    protected abstract String getAttackCommand ();

    protected abstract int getSshSessionMaxDuration ();

    private boolean requiredCapabilityMet (ShellSessionCapability required) {
        for (ShellSessionCapability actual : detectedCapabilities) {
            if (actual.getCapabilityType() == required.getCapabilityType()) {
                if (actual.hasAnOption(required.getCapabilityOptions())) {
                    return true;
                }
            }
        }
        return false;
    }
}
