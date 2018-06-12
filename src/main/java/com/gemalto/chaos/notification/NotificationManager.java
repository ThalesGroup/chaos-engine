package com.gemalto.chaos.notification;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class NotificationManager {

    @Autowired(required = false)
    private static List<NotificationMethods> notificationMethods;

    private NotificationManager() {
    }

    public static void sendNotification(String message) {
        if (notificationMethods != null) {
            for (NotificationMethods notif : notificationMethods) {
                notif.logEvent(message);
            }
        }
    }


}
