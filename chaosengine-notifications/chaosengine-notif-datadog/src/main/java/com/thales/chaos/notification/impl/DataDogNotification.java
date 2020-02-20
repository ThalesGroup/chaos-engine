/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
@ConditionalOnProperty(name = "datadog.enableEvents", havingValue = "true")
public class DataDogNotification implements NotificationMethods {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Collection<String> knownChaosEventFields = List.of("title", "message", "targetContainer");
    private static final String CONTAINER_TAG_PREFIX = "container.";
    private final Collection<String> ignoredContainerFields = List.of("knownMissingCapabilities");
    private final Collection<String> complexContainerFields = List.of("shellCapabilities");
    @Autowired
    private StatsDClient statsDClient;

    @Override
    public void logNotification (ChaosNotification notification) {
        DataDogEvent dataDogEvent = new DataDogEvent();
        send(dataDogEvent.buildFromNotification(notification), dataDogEvent.generateTags(notification));
    }

    void send (Event evt, Collection<String> tags) {
        try {
            log.info("Sending DataDog notification: {}, {}", v("notice", evt.getText()), v("tags", tags));
            statsDClient.recordEvent(evt, tags.toArray(String[]::new));
            log.debug("DataDog notification sent: {}, {}", v("notice", evt.getText()), v("tags", tags));
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
            Map<String, Object> fieldMap = chaosNotification.asMap();
            Optional.ofNullable(fieldMap.get("experimentId")).filter(String.class::isInstance).map(String.class::cast).ifPresent(evtBuilder::withAggregationKey);
            Optional.ofNullable(fieldMap.get("notificationLevel"))
                    .filter(NotificationLevel::isNotificationLevel)
                    .map(Object::toString)
                    .map(NotificationLevel::valueOf)
                    .map(this::mapLevel)
                    .ifPresent(evtBuilder::withAlertType);
            Optional.ofNullable(fieldMap.get("title")).filter(String.class::isInstance).map(String.class::cast).ifPresent(evtBuilder::withTitle);
            Optional.ofNullable(fieldMap.get("message")).filter(String.class::isInstance).map(String.class::cast).ifPresent(evtBuilder::withText);
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
            Map<String, Object> fieldMap = chaosNotification.asMap();
            Collection<String> tags = collectContainerTags(fieldMap);
            fieldMap.keySet().stream().map(Object::toString).filter(not(knownChaosEventFields::contains)).forEach(k -> tags.add(k + ":" + fieldMap.get(k)));
            return tags;
        }

        Collection<String> collectContainerTags (Map<String, Object> fieldMap) {
            Collection<String> tags = new ArrayList<>();
            Object container = fieldMap.get("targetContainer");
            Optional.ofNullable(container)
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<String, Object>) o)
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    .filter(e -> !ignoredContainerFields.contains(e.getKey()))
                    .filter(e -> !complexContainerFields.contains(e.getKey()))
                    .map(e -> CONTAINER_TAG_PREFIX + e.getKey() + ":" + e.getValue())
                    .forEach(tags::add);
            Optional.ofNullable(container)
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<String, Map<String, Object>>) o)
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    .filter(field -> !ignoredContainerFields.contains(field.getKey()))
                    .filter(field -> complexContainerFields.contains(field.getKey()))
                    .flatMap(stringObjectEntry -> stringObjectEntry.getValue()
                                                                   .entrySet()
                                                                   .stream()
                                                                   .map(e -> (Map.Entry) e)
                                                                   .map(s -> CONTAINER_TAG_PREFIX + stringObjectEntry.getKey() + "." + s
                                                                           .getKey() + ":" + s.getValue()))
                    .forEach(tags::add);
            return tags;
        }
    }
}
