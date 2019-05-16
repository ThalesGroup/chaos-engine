package com.thales.chaos.platform.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.thales.chaos.constants.CloudFoundryConstants;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static com.thales.chaos.exception.enums.CloudFoundryChaosErrorCode.EMPTY_RESPONSE;

public abstract class CloudFoundryPlatform extends Platform {
    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    @Autowired
    public CloudFoundryPlatform () {
    }


    @Autowired(required = false)
    void setCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
        this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
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
                                                                                        .equals(CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED));
        if (runningInstances.filter(a -> a.getInstances() > 0)
                            .filter(a -> a.getRunningInstances() == 0)
                            .hasElements()
                            .blockOptional()
                            .orElseThrow(EMPTY_RESPONSE.asChaosException())) {
            return PlatformHealth.FAILED;
        } else if (runningInstances.filter(a -> a.getInstances() > 0)
                                   .filter(a -> a.getRunningInstances() < a.getInstances())
                                   .hasElements()
                                   .blockOptional()
                                   .orElseThrow(EMPTY_RESPONSE.asChaosException())) {
            return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
    }

    @Override
    public List<Container> generateRoster () {
        return new ArrayList<>();
    }

    public void restageApplication (RestageApplicationRequest restageApplicationRequest) {
        cloudFoundryOperations.applications().restage(restageApplicationRequest).block();
    }

}
