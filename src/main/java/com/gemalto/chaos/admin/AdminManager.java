package com.gemalto.chaos.admin;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.enums.AdminState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class AdminManager {
    private static final Logger log = LoggerFactory.getLogger(AdminManager.class);
    private static AdminState adminState = AdminState.STARTING;
    private static Instant stateTimer = Instant.now();

    private AdminManager () {
    }

    public static AdminState getAdminState () {
        log.debug("Current admin state: {}", adminState);
        return adminState;
    }

    public static void setAdminState (AdminState newAdminState) {
        if (newAdminState != adminState && AdminState.getInvalidTransitions(newAdminState).contains(adminState)) {
            throw new ChaosException("Cannot transition from " + adminState + " to " + newAdminState + ".");
        }
        setAdminStateInner(newAdminState);
    }

    private static void setAdminStateInner (AdminState newAdminState) {
        stateTimer = Instant.now();
        adminState = newAdminState;
    }

    public static Duration getTimeInState () {
        return timeInState();
    }

    private static Duration timeInState () {
        return Duration.between(stateTimer, Instant.now());
    }

    public static boolean canRunAttacks () {
        return AdminState.getAttackStates().contains(adminState);
    }
}
