package com.gemalto.chaos.admin.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum AdminState {
    STARTING,
    STARTED,
    PAUSED,
    SHUTTING_DOWN,
    FINISHED;

    private static EnumMap<AdminState, EnumSet<AdminState>> invalidTransitions;

    public static Set<AdminState> getAttackStates() {
        return EnumSet.of(STARTED);
    }

    public static Set<AdminState> getHealthyStates() {
        return EnumSet.of(STARTED, PAUSED);
    }

    private static void initializeInvalidTransitions() {
        if (invalidTransitions == null) {
            invalidTransitions = new EnumMap<>(AdminState.class);
            invalidTransitions.put(STARTING, EnumSet.of(STARTING, STARTED, PAUSED, SHUTTING_DOWN, FINISHED));
            invalidTransitions.put(STARTED, EnumSet.of(STARTED, SHUTTING_DOWN, FINISHED));
            invalidTransitions.put(PAUSED, EnumSet.of(STARTING, PAUSED, SHUTTING_DOWN, FINISHED));
            invalidTransitions.put(SHUTTING_DOWN, EnumSet.of(STARTING, SHUTTING_DOWN, FINISHED));
            invalidTransitions.put(FINISHED, EnumSet.of(STARTING, STARTED, PAUSED, FINISHED));
        }
    }

    public static Set<AdminState> getInvalidTransitions(AdminState fromState) {
        initializeInvalidTransitions();
        return invalidTransitions.get(fromState);
    }
}
