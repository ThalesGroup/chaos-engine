package com.gemalto.chaos.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotification implements NotificationMethods {

    private final static Logger log = LoggerFactory.getLogger(ConsoleNotification.class);

    public ConsoleNotification() {
        log.info("Creating console logger");
    }

    @Override
    public void logEvent(String event) {
        log.info(event);

    }
}
