package com.thales.chaos.notification;

public interface NotificationMethods {
    void logEvent (ChaosExperimentEvent event);

    void logMessage (ChaosNotification msg);
}
