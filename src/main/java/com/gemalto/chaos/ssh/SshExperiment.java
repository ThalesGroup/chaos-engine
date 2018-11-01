package com.gemalto.chaos.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public abstract class SshExperiment {
    private static final Logger log = LoggerFactory.getLogger(SshExperiment.class);
    protected ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    protected ArrayList<ShellSessionCapability> detectedCapabilities;
    protected SshManager sshManager;

    protected abstract void buildRequiredCapabilities ();

    public boolean runExperiment (SshManager sshManager) throws IOException {
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
            log.debug("Starting {} experiment.", getExperimentName());
            sshManager.executeCommandInInteractiveShell(getExperimentCommand(), getExperimentName(), getSshSessionMaxDuration());
            log.debug("Experiment {} deployed.", getExperimentName());
            return true;
        } else {
            log.warn("Cannot execute SSH experiment {}. Current shell session does not have all required capabilities: {}, actual capabilities: {}", getExperimentName(), requiredCapabilities, detectedCapabilities);
        }
        return false;
    }

    private void collectAvailableCapabilities () throws IOException {
        ShellSessionCapabilityProvider capProvider = new ShellSessionCapabilityProvider(sshManager, requiredCapabilities);
        capProvider.build();
        detectedCapabilities = capProvider.getCapabilities();
    }

    private boolean availableCapabilitiesOutdated () {
        for (ShellSessionCapability requiredCap : requiredCapabilities) {
            if (!shellHasCapability(requiredCap)) {
                return true;
            }
        }
        return false;
    }

    private void updateAvailableCapabilities () throws IOException {
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

    public boolean shellHasRequiredCapabilities () {
        log.debug("Checking SSH experiment required capabilities");
        for (ShellSessionCapability required : requiredCapabilities) {
            if (!requiredCapabilityMet(required)) {
                log.debug("SSH Session does not have capability: {}", required);
                return false;
            }
        }
        return true;
    }

    protected abstract String getExperimentName ();

    protected abstract String getExperimentCommand ();

    protected abstract int getSshSessionMaxDuration ();

    private boolean shellHasCapability (ShellSessionCapability requiredCap) {
        for (ShellSessionCapability actualCap : detectedCapabilities) {
            if (actualCap.getCapabilityType() == requiredCap.getCapabilityType() && actualCap.hasAnOption(requiredCap.getCapabilityOptions())) {
                return true;
            }
        }
        return false;
    }

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

    public ArrayList<ShellSessionCapability> getShellSessionCapabilities () {
        return detectedCapabilities;
    }

    public void setShellSessionCapabilities (ArrayList<ShellSessionCapability> detectedCapabilities) {
        this.detectedCapabilities = detectedCapabilities;
    }
}
