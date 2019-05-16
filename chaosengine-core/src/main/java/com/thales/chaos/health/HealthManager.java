package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class HealthManager {
    public static final Logger log = LoggerFactory.getLogger(HealthManager.class);
    @Autowired(required = false)
    private Collection<SystemHealth> systemHealth;

    @Autowired
    HealthManager () {
    }

    HealthManager (Collection<SystemHealth> systemHealth) {
        this.systemHealth = systemHealth;
    }

    SystemHealthState getHealth () {
        if (getSystemHealth() != null) {
            for (SystemHealth health : getSystemHealth()) {
                SystemHealthState healthCheck = health.getHealth();
                if (healthCheck == SystemHealthState.OK) {
                    log.debug("{} : {}", health.getClass().getSimpleName(), healthCheck);
                    continue;
                }
                log.error("{} : {}", health.getClass().getSimpleName(), healthCheck);
                return SystemHealthState.ERROR;
            }
            return SystemHealthState.OK;
        }
        return SystemHealthState.UNKNOWN;
    }

    Collection<SystemHealth> getSystemHealth () {
        return systemHealth;
    }
}
