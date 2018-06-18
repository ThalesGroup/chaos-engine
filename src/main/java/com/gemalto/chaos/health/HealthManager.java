package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HealthManager {

    @Autowired(required = false)
    private Health systemHealth;

    SystemHealthState getHealth() {
        if (systemHealth != null) {
            return systemHealth.getHealth();
        }
        return SystemHealthState.UNKNOWN;
    }


}
