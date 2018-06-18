package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SystemHealth {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public SystemHealthState getHealth() {
        return SystemHealthState.OK;
    }

}
