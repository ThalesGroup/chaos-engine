package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "cf.organization" })
public class CloudFoundryHealth extends AbstractPlatformHealth {
    @Autowired
    private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;

    CloudFoundryHealth (CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform) {
        this();
        this.cloudFoundryApplicationPlatform = cloudFoundryApplicationPlatform;
    }

    @Autowired
    CloudFoundryHealth () {
        log.debug("Using CloudFoundry API check for health check.");
    }

    @Override
    Platform getPlatform () {
        return cloudFoundryApplicationPlatform;
    }
}
