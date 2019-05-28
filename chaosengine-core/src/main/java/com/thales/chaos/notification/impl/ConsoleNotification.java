package com.thales.chaos.notification.impl;

import com.thales.chaos.notification.ChaosExperimentEvent;
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
    public void logEvent (ChaosExperimentEvent event) {
        log.debug("{}", StructuredArguments.value(event.getClass().getSimpleName(), event));
    }

    @Override
    public void logMessage (ChaosNotification msg) {
        log.debug("{}", StructuredArguments.value(msg.getClass().getSimpleName(), msg));
    }
}

