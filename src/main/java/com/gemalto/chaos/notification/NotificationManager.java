package com.gemalto.chaos.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationManager {
    @Autowired(required = false)
    private List<NotificationMethods> notificationMethods;

    private NotificationManager () {
    }

    public void sendNotification (ChaosEvent chaosEvent) {
        if (notificationMethods != null) {
            for (NotificationMethods notif : notificationMethods) {
                notif.logEvent(chaosEvent);
            }
        }
    }
}
