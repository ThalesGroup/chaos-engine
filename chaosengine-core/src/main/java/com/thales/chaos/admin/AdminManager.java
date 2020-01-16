/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.admin;

import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosMessage;
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
        setAdminState(newAdminState, true);
    }

    void setAdminState (AdminState newAdminState, boolean logNewState) {
        setAdminStateInner(newAdminState);
        if (logNewState) {
            notificationManager.sendNotification(ChaosMessage.builder()
                                                             .withNotificationLevel(NotificationLevel.WARN)
                                                             .withTitle("State changed")
                                                             .withMessage("Chaos Engine admin state has changed: " + newAdminState
                                                                     .name())
                                                             .build());
        }
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
        setAdminState(AdminState.STARTED, false);
        notificationManager.sendNotification(ChaosMessage.builder()
                                                         .withNotificationLevel(NotificationLevel.GOOD)
                                                         .withTitle("Engine Started")
                                                         .withMessage("Chaos Engine has been started")
                                                         .build());
    }

    private void setAdminStateInner (AdminState newAdminState) {
        stateTimer = Instant.now();
        adminState = newAdminState;
    }

    public boolean mustRunSelfHealing () {
        return AdminState.ABORT.equals(adminState);
    }
}
