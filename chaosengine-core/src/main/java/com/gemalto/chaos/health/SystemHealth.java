package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;

public interface SystemHealth {
    SystemHealthState getHealth ();
}
