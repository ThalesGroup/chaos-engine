package com.gemalto.chaos.notification;

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

    public void sendNotification (ChaosEvent chaosEvent) {
        if (notificationMethods != null) {
            for (NotificationMethods notif : notificationMethods) {
                notif.logEvent(chaosEvent);
            }
        }
    }

    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 30)
    public void flushBuffers () {
        if (bufferedNotificationMethods != null) {
            bufferedNotificationMethods.forEach(BufferedNotificationMethod::flushBuffer);
        }
    }
}
