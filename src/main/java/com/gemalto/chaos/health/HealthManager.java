package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class HealthManager {

    @Autowired(required = false)
    private Set<SystemHealth> systemHealth;

    SystemHealthState getHealth() {
        if (systemHealth != null) {
            for (SystemHealth health : systemHealth) {
                if (health.getHealth() == SystemHealthState.OK) {
                    continue;
                }
                return SystemHealthState.ERROR;
            }
            return SystemHealthState.OK;
        }
        return SystemHealthState.UNKNOWN;
    }


}
