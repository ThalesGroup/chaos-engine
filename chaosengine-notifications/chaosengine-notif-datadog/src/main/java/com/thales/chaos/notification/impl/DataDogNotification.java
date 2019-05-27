package com.thales.chaos.notification.impl;

import com.thales.chaos.notification.ChaosExperimentEvent;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.NotificationMethods;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotification implements NotificationMethods {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private StatsDClient statsDClient;

    @Override
    public void logEvent (ChaosExperimentEvent event) {
        DataDogEvent dataDogEvent = new DataDogEvent();
        List<String> tags = dataDogEvent.generateTags(event);
        tags.add("target:" + event.getTargetContainer().getSimpleName());
        tags.add("aggregationidentifier:" + event.getTargetContainer().getAggregationIdentifier());
        tags.add("platform:" + event.getTargetContainer().getPlatform().getPlatformType());
        tags.add("container:" + event.getTargetContainer().getContainerType());
        send(dataDogEvent.buildFromEvent(event), tags);
    }

    @Override
    public void logMessage (ChaosNotification msg) {
        DataDogEvent dataDogEvent = new DataDogEvent();
        send(dataDogEvent.buildFromNotification(msg), dataDogEvent.generateTags(msg));
    }

    void send (Event evt, List<String> tags) {
        try {
            log.debug("Sending DataDog notification");
            statsDClient.recordEvent(evt, tags.toArray(new String[0]));
            log.debug("DataDog notification send");
        } catch (StatsDClientException ex) {
            log.warn("Cannot send DataDog notification", ex);
        }
    }
    public DataDogNotification () {
        log.info("DataDog notification channel created");
    }

    DataDogNotification(StatsDClient statsDClient){
        this.statsDClient=statsDClient;
    }

    class DataDogEvent {
        protected static final String EVENT_PREFIX = "Chaos Event ";
        protected static final String SOURCE_TYPE = "JAVA";

        Event buildFromEvent (ChaosExperimentEvent chaosExperimentEvent) {
            return Event.builder()
                        .withAggregationKey(chaosExperimentEvent.getExperimentId())
                        .withAlertType(mapLevel(chaosExperimentEvent.getNotificationLevel()))
                        //TODO : replace this line with getTitle call
                        .withTitle(EVENT_PREFIX + chaosExperimentEvent.getExperimentMethod())
                        .withText(chaosExperimentEvent.getMessage())
                        .withSourceTypeName(SOURCE_TYPE)
                        .build();
        }

        Event buildFromNotification (ChaosNotification chaosNotification) {
            return Event.builder()
                        .withAlertType(mapLevel(chaosNotification.getNotificationLevel()))
                        .withTitle(chaosNotification.getTitle())
                        .withText(chaosNotification.getMessage())
                        .withSourceTypeName(SOURCE_TYPE)
                        .build();
        }

        Event.AlertType mapLevel (NotificationLevel level) {
            switch (level) {
                case ERROR:
                    return Event.AlertType.ERROR;
                case WARN:
                    return Event.AlertType.WARNING;
                default:
                    return Event.AlertType.SUCCESS;
            }
        }

        List<String> generateTags (ChaosNotification chaosNotification) {
            ArrayList<String> tags = new ArrayList<>();
            for (Field field : chaosNotification.getClass().getDeclaredFields()) {
                boolean usedField = true;
                field.setAccessible(true);
                try {
                    if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()) || field.get(chaosNotification) == null) {
                        usedField = false;
                    }
                } catch (IllegalAccessException e) {
                    usedField = false;
                }
                if (!usedField) continue;
                try {
                    tags.add(field.getName() + ":" + field.get(chaosNotification));
                } catch (IllegalAccessException e) {
                    tags.add(field.getName() + ":IllegalAccessException");
                }
            }
            return tags;
        }
    }
}
