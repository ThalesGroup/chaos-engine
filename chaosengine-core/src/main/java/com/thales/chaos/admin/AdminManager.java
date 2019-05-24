package com.thales.chaos.admin;

import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.notification.ChaosMessage;
import com.thales.chaos.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private NotificationManager notificationManager;

    public AdminState getAdminState () {
        log.debug("Current admin state: {}", adminState);
        return adminState;
    }
    void setAdminState (AdminState newAdminState) {
        setAdminStateInner(newAdminState);
        notificationManager.sendNotification(ChaosMessage.builder()
                                                         .withTitle("State changed")
                                                         .withMessage("Chaos Engine admin state has changed: " + newAdminState
                                                                 .name())
                                                         .build());
    }

    public Duration getTimeInState () {
        return timeInState();
    }

    private Duration timeInState () {
        return Duration.between(stateTimer, Instant.now());
    }

    public boolean canRunExperiments () {
        return AdminState.getExperimentStates().contains(adminState);
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

    public boolean mustRunSelfHealing () {
        return AdminState.ABORT.equals(adminState);
    }
}
