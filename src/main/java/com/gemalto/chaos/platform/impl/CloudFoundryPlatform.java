package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.ssh.SshAttack;
import com.gemalto.chaos.ssh.impl.CloudFoundrySshManager;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryPlatform extends Platform {
    private CloudFoundryOperations cloudFoundryOperations;
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;

    @Autowired
    public CloudFoundryPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    private CloudFoundryPlatform () {
    }

    @Autowired(required = false)
    private void setCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
        this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
    }

    public static CloudFoundryPlatformBuilder builder () {
        return CloudFoundryPlatformBuilder.builder();
    }

    public CloudFoundryPlatformInfo getCloudFoundryPlatformInfo () {
        cloudFoundryPlatformInfo.fetchInfo();
        return cloudFoundryPlatformInfo;
    }


    @Override
    public List<Container> generateRoster () {
        List<Container> containers = new ArrayList<>();
        return containers;
    }

    public boolean isChaosEngine (String applicationName) {
        return cloudFoundrySelfAwareness != null && (cloudFoundrySelfAwareness.isMe(applicationName) || cloudFoundrySelfAwareness
                .isFriendly(applicationName));
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            cloudFoundryOperations.applications().list();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            log.error("Failed to load application list", e);
            return ApiStatus.ERROR;
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.PAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        Flux<ApplicationSummary> runningInstances = cloudFoundryOperations.applications()
                                                                          .list()
                                                                          .filter(a -> a.getRequestedState()
                                                                                        .equals(CLOUDFOUNDRY_APPLICATION_STARTED));
        if (runningInstances.filter(a -> a.getInstances() > 0)
                            .filter(a -> a.getRunningInstances() == 0)
                            .hasElements()
                            .block()) {
            return PlatformHealth.FAILED;
        } else if (runningInstances.filter(a -> a.getInstances() > 0)
                                   .filter(a -> a.getRunningInstances() < a.getInstances())
                                   .hasElements()
                                   .block()) {
            return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
    }

    public void sshAttack (SshAttack attack, CloudFoundryContainer container) {
        CloudFoundrySshManager ssh = new CloudFoundrySshManager(getCloudFoundryPlatformInfo());
        if (ssh.connect(container)) {
            if (container.getDetectedCapabilities() != null) {
                attack.setShellSessionCapabilities(container.getDetectedCapabilities());
            }
            attack.attack(ssh);
            if (container.getDetectedCapabilities() == null) {
                container.setDetectedCapabilities(attack.getShellSessionCapabilities());
            }
            ssh.disconnect();
        }
    }



    public static final class CloudFoundryPlatformBuilder {
        private CloudFoundryOperations cloudFoundryOperations;
        private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
        private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

        private CloudFoundryPlatformBuilder () {
        }

        private static CloudFoundryPlatformBuilder builder () {
            return new CloudFoundryPlatformBuilder();
        }

        CloudFoundryPlatformBuilder withCloudFoundryOperations (CloudFoundryOperations cloudFoundryOperations) {
            this.cloudFoundryOperations = cloudFoundryOperations;
            return this;
        }

        CloudFoundryPlatformBuilder withCloudFoundryPlatformInfo (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
            this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
            return this;
        }

        CloudFoundryPlatformBuilder withCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
            this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
            return this;
        }

        public CloudFoundryPlatform build () {
            CloudFoundryPlatform cloudFoundryPlatform = new CloudFoundryPlatform();
            cloudFoundryPlatform.cloudFoundrySelfAwareness = this.cloudFoundrySelfAwareness;
            cloudFoundryPlatform.cloudFoundryOperations = this.cloudFoundryOperations;
            cloudFoundryPlatform.cloudFoundryPlatformInfo = this.cloudFoundryPlatformInfo;
            return cloudFoundryPlatform;
        }
    }
}
