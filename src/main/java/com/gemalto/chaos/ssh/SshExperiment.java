package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public abstract class SshExperiment {
    private static final Logger log = LoggerFactory.getLogger(SshExperiment.class);
    private static final String DEFAULT_UPLOAD_PATH="/tmp/";
    private  String experimentName;
    private  String experimentScript;
    protected ArrayList<ShellSessionCapability> requiredCapabilities = new ArrayList<>();
    protected ArrayList<ShellSessionCapability> detectedCapabilities;
    protected SshManager sshManager;
    private ShResourceService shResourceService;

    protected abstract void buildRequiredCapabilities ();

    public String getExperimentName () {
        return experimentName;
    }


    public  String getExperimentScript () {
        return experimentScript;
    }

    private void buildGenerallyRequiredCapabilities(){
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.ASH)
                                                                                      .addCapabilityOption(ShellSessionCapabilityOption.SH));
        requiredCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.TYPE));
    }

    protected SshExperiment(String experimentName,String experimentScript){
        this.experimentName=experimentName;
        this.experimentScript=experimentScript;
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

    public boolean runExperiment () throws IOException {
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
            sshManager.uploadResource(shResourceService.getScriptResource(getExperimentScript()),DEFAULT_UPLOAD_PATH,true);
            log.debug("Experiment {} deployed.", getExperimentName());
            String scriptExec=String.format("nohup %s%s &",DEFAULT_UPLOAD_PATH,getExperimentScript());
            sshManager.executeCommandInShell(scriptExec, getExperimentName());
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
