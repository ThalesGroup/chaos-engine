package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;

public interface SystemHealth {
    SystemHealthState getHealth ();
}
