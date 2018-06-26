package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.health.SystemHealth;
import com.gemalto.chaos.health.enums.SystemHealthState;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(CloudFoundryOperations.class)
public class CloudFoundryHealth implements SystemHealth {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundryHealth.class);
    @Autowired(required = false)
    private CloudFoundryPlatform cloudFoundryPlatform;

    @Autowired
    CloudFoundryHealth () {
        log.debug("Using CloudFoundry API check for health check.");
    }

    CloudFoundryHealth (CloudFoundryPlatform cloudFoundryPlatform) {
        this.cloudFoundryPlatform = cloudFoundryPlatform;
    }

    @Override
    public SystemHealthState getHealth () {
        try {
            switch (cloudFoundryPlatform.getApiStatus()) {
                case OK:
                    return SystemHealthState.OK;
                case ERROR:
                    return SystemHealthState.ERROR;
                default:
                    return SystemHealthState.UNKNOWN;
            }
        } catch (RuntimeException e) {
            log.error("Could not resolve API Status. Returning error", e);
            return SystemHealthState.UNKNOWN;
        }
    }
}
