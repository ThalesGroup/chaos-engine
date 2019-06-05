package com.thales.chaos.notification.impl;

import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.NotificationMethods;
import net.logstash.logback.argument.StructuredArguments;
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
    public void logNotification (ChaosNotification notification) {
        log.debug(notification.getMessage(), StructuredArguments.value(notification.getClass()
                                                                                   .getSimpleName(), notification));
    }
}

