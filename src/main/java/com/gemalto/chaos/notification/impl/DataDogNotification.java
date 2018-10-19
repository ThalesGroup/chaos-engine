package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationMethods;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotification implements NotificationMethods {
    private static final int statsdPort = 8125;
    private static final String statsdHost = "datadog";
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void logEvent (ChaosEvent event) {
        DataDogEvent dataDogEvent = new DataDogEvent(event);
        try {
            dataDogEvent.send();
        } catch (StatsDClientException ex) {
            log.warn("Cannot send DataDog event: {}", ex);
        }
    }

    protected class DataDogEvent {
        private ChaosEvent chaosEvent;
        protected final String eventPrefix="Chaos Event ";
        public DataDogEvent (ChaosEvent event) {
            this.chaosEvent = event;
        }

        protected Event buildEvent(){
            return Event.builder()
                        .withAggregationKey(chaosEvent.getExperimentId())
                        .withAlertType(mapLevel(chaosEvent.getNotificationLevel()))
                        .withTitle(eventPrefix + chaosEvent.getExperimentMethod())
                        .withText(chaosEvent.getMessage())
                        .withSourceTypeName("Java")
                        .build();
        }

        protected Event.AlertType mapLevel (NotificationLevel level) {
            switch (level) {
                case ERROR:
                    return Event.AlertType.ERROR;
                case WARN:
                    return Event.AlertType.WARNING;
                default:
                    return Event.AlertType.SUCCESS;
            }
        }

        public void send () {
            StatsDClient statsd = new NonBlockingStatsDClient("", statsdHost, statsdPort, generateTags());
            statsd.recordEvent(buildEvent());
            statsd.close();
        }

        protected String[] generateTags () {
            String[] tags = new String[5];
            tags[0] = "service:chaosengine";
            tags[1] = "ExperimentId:" + chaosEvent.getExperimentId();
            tags[2] = "Method:" + chaosEvent.getExperimentMethod();
            tags[3] = "Type:" + chaosEvent.getExperimentType().name();
            tags[4] = "Target:" + chaosEvent.getTargetContainer().getSimpleName();
            return tags;
        }
    }
}
