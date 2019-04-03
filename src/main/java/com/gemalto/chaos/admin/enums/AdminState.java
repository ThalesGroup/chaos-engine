package com.gemalto.chaos.admin.enums;

import java.util.EnumSet;
import java.util.Set;

public enum AdminState {
    STARTING,
    STARTED,
    PAUSED,
    DRAIN,
    ABORT,
    ;

    public static Set<AdminState> getExperimentStates () {
        return EnumSet.of(STARTED);
    }

    public static Set<AdminState> getSelfHealingStates () {
        return EnumSet.of(STARTED, DRAIN, ABORT);
    }

    public static Set<AdminState> getHealthyStates () {
        return EnumSet.of(STARTED, DRAIN, PAUSED, ABORT);
    }
}
