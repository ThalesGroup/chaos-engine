package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.health.SystemHealth;
import com.gemalto.chaos.health.enums.SystemHealthState;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import com.gemalto.chaos.services.impl.CloudFoundryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(CloudFoundryService.class)
public class CloudFoundryHealth extends SystemHealth {

    @Autowired
    CloudFoundryHealth() {
    }

    CloudFoundryHealth(CloudFoundryPlatform cloudFoundryPlatform) {
        this.cloudFoundryPlatform = cloudFoundryPlatform;
    }

    @Autowired(required = false)
    private CloudFoundryPlatform cloudFoundryPlatform;


    @Override
    public SystemHealthState getHealth() {
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
