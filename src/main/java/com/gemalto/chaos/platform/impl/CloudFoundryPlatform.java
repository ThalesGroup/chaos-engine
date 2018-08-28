package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

public class CloudFoundryPlatform extends Platform {
    private CloudFoundryOperations cloudFoundryOperations;
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    @Autowired
    public CloudFoundryPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    public CloudFoundryPlatform () {
    }

    public static CloudFoundryPlatformBuilder getBuilder () {
        return CloudFoundryPlatformBuilder.builder();
    }

    @Autowired(required = false)
    void setCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
        this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
    }

    public CloudFoundryPlatformInfo getCloudFoundryPlatformInfo () {
        cloudFoundryPlatformInfo.fetchInfo();
        return cloudFoundryPlatformInfo;
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

    @Override
    public List<Container> generateRoster () {
        List<Container> containers = new ArrayList<>();
        return containers;
    }

    public void restageApplication (RestageApplicationRequest restageApplicationRequest) {
        cloudFoundryOperations.applications().restage(restageApplicationRequest).block();
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
