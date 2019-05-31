package com.thales.chaos.notification.impl;

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

import java.util.*;

import static java.util.function.Predicate.not;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotification implements NotificationMethods {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Collection<String> knownChaosEventFields = List.of("title", "message", "targetContainer");

    @Autowired
    private StatsDClient statsDClient;

    @Override
    public void logNotification (ChaosNotification notification) {
        DataDogEvent dataDogEvent = new DataDogEvent();
        send(dataDogEvent.buildFromNotification(notification), dataDogEvent.generateTags(notification));
    }

    void send (Event evt, Collection<String> tags) {
        try {
            log.debug("Sending DataDog notification: {}, {}", v("notice", evt.getText()), v("tags", tags));
            statsDClient.recordEvent(evt, tags.toArray(String[]::new));
            log.debug("DataDog notification send: {}, {}", v("notice", evt.getText()), v("tags", tags));
        } catch (StatsDClientException ex) {
            log.error("Cannot send DataDog notification: {}, {}", v("notice", evt.getText()), v("tags", tags));
        }
    }
    public DataDogNotification () {
        log.info("DataDog notification channel created");
    }

    DataDogNotification(StatsDClient statsDClient){
        this.statsDClient=statsDClient;
    }

    class DataDogEvent {
        protected static final String SOURCE_TYPE = "JAVA";

        Event buildFromNotification (ChaosNotification chaosNotification) {
            Event.Builder evtBuilder = Event.builder();
            Map<Object, Object> fieldMap = chaosNotification.asMap();
            Optional.ofNullable(fieldMap.get("experimentId"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .ifPresent(evtBuilder::withAggregationKey);
            Optional.ofNullable(fieldMap.get("notificationLevel"))
                    .filter(NotificationLevel::isNotificationLevel)
                    .map(Object::toString)
                    .map(NotificationLevel::valueOf)
                    .map(this::mapLevel)
                    .ifPresent(evtBuilder::withAlertType);
            Optional.ofNullable(fieldMap.get("title"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .ifPresent(evtBuilder::withTitle);
            Optional.ofNullable(fieldMap.get("message"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .ifPresent(evtBuilder::withText);
            evtBuilder.withSourceTypeName(SOURCE_TYPE);
            return evtBuilder.build();
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

        Collection<String> generateTags (ChaosNotification chaosNotification) {
            Map<Object, Object> fieldMap = chaosNotification.asMap();
            Collection<String> tags = new ArrayList<>(collectContainerTags(fieldMap));

            fieldMap.keySet()
                    .stream()
                    .map(Object::toString)
                    .filter(not(knownChaosEventFields::contains))
                    .forEach(k -> tags.add(k + ":" + fieldMap.get(k)));
            return tags;
        }

        Collection<String> collectContainerTags (Map<Object, Object> fieldMap) {
            Collection<String> tags = new ArrayList<>();
            Optional.ofNullable(fieldMap.get("targetContainer"))
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<Object, Object>) o)
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    .map(e -> "container." + e.getKey() + ":" + e.getValue())
                    .forEach(tags::add);
            return tags;
        }
    }
}
