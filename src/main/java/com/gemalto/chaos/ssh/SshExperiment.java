package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SshExperiment {
    private static final Logger log = LoggerFactory.getLogger(SshExperiment.class);
    public static final String DEFAULT_UPLOAD_PATH = "/tmp/";
    private String experimentName;
    private String experimentScript;
    protected List<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    private List<ShellSessionCapability> detectedCapabilities;
    private SshManager sshManager;
    private ShResourceService shResourceService;

    protected abstract void buildRequiredCapabilities ();

    public String getExperimentName () {
        return experimentName;
    }

    public String getExperimentScript () {
        return experimentScript;
    }

    private void buildGenerallyRequiredCapabilities () {
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.ASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.SH));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.TYPE));
    }

    protected SshExperiment (String experimentName, String experimentScript) {
        this.experimentName = experimentName;
        this.experimentScript = experimentScript;
        buildGenerallyRequiredCapabilities();
    }

    public SshExperiment setSshManager (SshManager sshManager) {
        this.sshManager = sshManager;
        return this;
    }

    public SshExperiment setShResourceService (ShResourceService shResourceService) {
        this.shResourceService = shResourceService;
        return this;
    }

    public void runExperiment () throws IOException {
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
            log.debug("Deploying {} experiment.", getExperimentName());
            sshManager.uploadResource(shResourceService.getScriptResource(getExperimentScript()), DEFAULT_UPLOAD_PATH, true);
            log.debug("Experiment {} deployed.", getExperimentName());
            String scriptExec = String.format("nohup %s%s &", DEFAULT_UPLOAD_PATH, getExperimentScript());
            sshManager.executeCommandInShell(scriptExec, getExperimentName());
        }else {
            log.error("Cannot execute SSH experiment {}. Current shell session does not have all required capabilities: {}, actual capabilities: {}", getExperimentName(), requiredCapabilities, detectedCapabilities);
            throw new ChaosException("Cannot execute SSH experiment. Current shell session does not have all required capabilities");
        }
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
        List<ShellSessionCapability> additionalRequiredCapabilities = new ArrayList<>();
        List<ShellSessionCapability> additionalDetectedCapabilities;
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

    public List<ShellSessionCapability> getDetectedShellSessionCapabilities () {
        return detectedCapabilities;
    }

    public void setDetectedShellSessionCapabilities (List<ShellSessionCapability> detectedCapabilities) {
        this.detectedCapabilities = detectedCapabilities;
    }
}
