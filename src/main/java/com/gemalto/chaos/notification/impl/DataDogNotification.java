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
    private DataDogEventFactory dataDogEventFactory;

    @Override
    public void logEvent (ChaosEvent event) {
        DataDogEvent dataDogEvent = dataDogEventFactory.getDataDogEvent(event);
        try {
            dataDogEvent.send();
        } catch (StatsDClientException ex) {
            log.warn("Cannot send DataDog event: {}", ex);
        }
    }

    public DataDogNotification () {
        dataDogEventFactory = new DataDogEventFactory();
        log.info("DataDog notification channel created");
    }

    DataDogNotification (DataDogEventFactory factory) {
        dataDogEventFactory = factory;
    }

    class DataDogEventFactory {
        DataDogEvent getDataDogEvent (ChaosEvent event) {
            return new DataDogEvent(event);
        }
    }

    class StatsDClientFactory{
        StatsDClient getStatsDClient(String[] tags){
           return new NonBlockingStatsDClient("", statsdHost, statsdPort, tags);
        }
    }

    class DataDogEvent {
        private ChaosEvent chaosEvent;
        private final String eventPrefix = "Chaos Event ";
        private StatsDClientFactory factory;

        DataDogEvent (ChaosEvent event) {
            this.chaosEvent = event;
            factory = new StatsDClientFactory();
        }

        DataDogEvent (ChaosEvent event, StatsDClientFactory statsDClientFactory) {
            this.chaosEvent = event;
            factory = statsDClientFactory;
        }


        Event buildEvent () {
            return Event.builder()
                        .withAggregationKey(chaosEvent.getExperimentId())
                        .withAlertType(mapLevel(chaosEvent.getNotificationLevel()))
                        .withTitle(eventPrefix + chaosEvent.getExperimentMethod())
                        .withText(chaosEvent.getMessage())
                        .withSourceTypeName("Java")
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

        StatsDClient getStatsdClient (String[] tags) {
            return factory.getStatsDClient(tags);
        }

        void send () {
            try (StatsDClient statsd = getStatsdClient(generateTags())) {
                statsd.recordEvent(buildEvent());
            }
        }

        String[] generateTags () {
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
