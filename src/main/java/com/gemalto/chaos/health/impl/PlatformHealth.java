package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.health.SystemHealth;
import com.gemalto.chaos.health.enums.SystemHealthState;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PlatformHealth implements SystemHealth {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public SystemHealthState getHealth () {
        try {
            switch (getPlatform().getApiStatus()) {
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

    abstract Platform getPlatform ();
}