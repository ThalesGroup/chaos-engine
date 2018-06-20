package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.admin.enums.AdminState;
import com.gemalto.chaos.health.SystemHealth;
import com.gemalto.chaos.health.enums.SystemHealthState;

public class AdminHealth extends SystemHealth {

    @Override
    public SystemHealthState getHealth() {
        boolean healthyState = AdminState.getHealthyStates().contains(AdminManager.getAdminState());
        boolean longTimeInState = AdminManager.getTimeInState().getSeconds() > 60 * 60 * 24;


        return (!healthyState) && longTimeInState ? SystemHealthState.ERROR : SystemHealthState.OK;
    }
}
