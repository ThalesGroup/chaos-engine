package com.gemalto.chaos.admin;

import com.gemalto.chaos.admin.enums.AdminState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AdminManager {
    private static final Logger log = LoggerFactory.getLogger(AdminManager.class);
    private AdminState adminState = AdminState.STARTING;
    private Instant stateTimer = Instant.now();

    public AdminState getAdminState () {
        log.debug("Current admin state: {}", adminState);
        return adminState;
    }

    void setAdminState (AdminState newAdminState) {
        setAdminStateInner(newAdminState);
    }

    public Duration getTimeInState () {
        return timeInState();
    }

    private Duration timeInState () {
        return Duration.between(stateTimer, Instant.now());
    }

    public boolean canRunExperiments () {
        return AdminState.geExperimentStates().contains(adminState);
    }

    public boolean canRunSelfHealing () {
        return AdminState.getSelfHealingStates().contains(adminState);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void startupComplete () {
        setAdminState(AdminState.STARTED);
    }

    private void setAdminStateInner (AdminState newAdminState) {
        stateTimer = Instant.now();
        adminState = newAdminState;
    }
}
