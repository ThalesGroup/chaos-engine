package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotification implements NotificationMethods {
    private static final Logger log = LoggerFactory.getLogger(ConsoleNotification.class);

    public ConsoleNotification () {
        log.info("Creating console logger");
    }

    @Override
    public void logEvent (ChaosEvent event) {
        log.info("{}", event);
    }
}
