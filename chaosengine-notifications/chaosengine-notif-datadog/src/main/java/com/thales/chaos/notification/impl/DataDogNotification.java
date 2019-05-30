package com.thales.chaos.notification.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.NotificationMethods;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.platform.Platform;
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
    public void logEvent (ChaosExperimentEvent event) {
        logMessage(event);
    }

    @Override
    public void logMessage (ChaosNotification msg) {
        DataDogEvent dataDogEvent = new DataDogEvent();
        send(dataDogEvent.buildFromNotification(msg), dataDogEvent.generateTags(msg));
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
            Optional.ofNullable(chaosNotification.asMap().get("experimentId"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .ifPresent(evtBuilder::withAggregationKey);
            Optional.ofNullable(chaosNotification.asMap().get("notificationLevel"))
                    .filter(NotificationLevel::isNotificationLevel)
                    .map(Object::toString)
                    .map(NotificationLevel::valueOf)
                    .map(this::mapLevel)
                    .ifPresent(evtBuilder::withAlertType);
            Optional.ofNullable(chaosNotification.asMap().get("title"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .ifPresent(evtBuilder::withTitle);
            Optional.ofNullable(chaosNotification.asMap().get("message"))
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
            ArrayList<String> tags = new ArrayList<>(collectContainerTags(fieldMap));
            fieldMap.keySet()
                    .stream()
                    .map(Object::toString)
                    .filter(not(knownChaosEventFields::contains))
                    .forEach(k -> tags.add(k + ":" + fieldMap.get(k)));
            return tags;
        }

        Collection<String> collectContainerTags (Map<Object, Object> fieldMap) {
            ArrayList<String> tags = new ArrayList<>();
            Optional.ofNullable(fieldMap.get("targetContainer"))
                    .filter(Container.class::isInstance)
                    .map(Container.class::cast)
                    .map(Container::getSimpleName)
                    .map(s -> "target:" + s)
                    .ifPresent(tags::add);
            Optional.ofNullable(fieldMap.get("aggregationidentifier"))
                    .filter(Container.class::isInstance)
                    .map(Container.class::cast)
                    .map(Container::getAggregationIdentifier)
                    .map(s -> "aggregationidentifier:" + s)
                    .ifPresent(tags::add);
            Optional.ofNullable(fieldMap.get("platform"))
                    .filter(Container.class::isInstance)
                    .map(Container.class::cast)
                    .map(Container::getPlatform)
                    .map(Platform::getPlatformType)
                    .map(s -> "platform:" + s)
                    .ifPresent(tags::add);
            Optional.ofNullable(fieldMap.get("containertype"))
                    .filter(Container.class::isInstance)
                    .map(Container.class::cast)
                    .map(Container::getContainerType)
                    .map(s -> "containertype:" + s)
                    .ifPresent(tags::add);
            return tags;
        }
    }
}
