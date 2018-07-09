package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryHealth extends AbstractPlatformHealth {
    @Autowired
    private CloudFoundryPlatform cloudFoundryPlatform;

    CloudFoundryHealth (CloudFoundryPlatform cloudFoundryPlatform) {
        this();
        this.cloudFoundryPlatform = cloudFoundryPlatform;
    }

    @Autowired
    CloudFoundryHealth () {
        log.debug("Using CloudFoundry API check for health check.");
    }

    @Override
    Platform getPlatform () {
        return cloudFoundryPlatform;
    }
}
