package com.thales.chaos.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class NotificationManager {
    @Autowired(required = false)
    private Collection<NotificationMethods> notificationMethods;
    @Autowired(required = false)
    private Collection<BufferedNotificationMethod> bufferedNotificationMethods;

    @Autowired
    private NotificationManager () {
    }

    NotificationManager (Collection<NotificationMethods> notificationMethods) {
        this.notificationMethods = notificationMethods;
    }

    public void sendNotification (ChaosNotification chaosNotification) {
        if (notificationMethods != null) {
            for (NotificationMethods notif : notificationMethods) {
                if (chaosNotification instanceof ChaosExperimentEvent) {
                    notif.logEvent((ChaosExperimentEvent) chaosNotification);
                } else {
                    notif.logMessage(chaosNotification);
                }
            }
        }
    }

    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 5)
    public void flushBuffers () {
        if (bufferedNotificationMethods != null) {
            bufferedNotificationMethods.forEach(BufferedNotificationMethod::flushBuffer);
        }
    }
}
