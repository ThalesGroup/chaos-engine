package com.gemalto.chaos.admin.enums;

import java.util.EnumSet;

public enum AdminState {
    STARTING,
    STARTED,
    PAUSED,
    SHUTTING_DOWN,
    FINISHED;

    public static EnumSet<AdminState> getAttackStates() {
        return EnumSet.of(STARTED);
    }

    public static EnumSet<AdminState> getHealthyStates() {
        return EnumSet.of(STARTED, PAUSED);
    }
}
