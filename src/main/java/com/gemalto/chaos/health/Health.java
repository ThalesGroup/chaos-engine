package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;

abstract class Health {

    SystemHealthState getHealth() {
        return SystemHealthState.OK;
    }
}
