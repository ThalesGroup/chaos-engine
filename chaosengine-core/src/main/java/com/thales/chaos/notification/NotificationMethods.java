package com.thales.chaos.notification;

import com.thales.chaos.notification.message.ChaosExperimentEvent;

public interface NotificationMethods {
    void logEvent (ChaosExperimentEvent event);

    void logMessage (ChaosNotification msg);
}
