package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationMethods;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_EXPERIMENTID_KEY;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotification implements NotificationMethods {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private StatsDClient statsDClient;

    @Override
    public void logEvent (ChaosEvent event) {
        DataDogEvent dataDogEvent = new DataDogEvent(event);
        try {
            log.debug("Sending DataDog notification", keyValue(DATADOG_EXPERIMENTID_KEY, event.getExperimentId()));
            dataDogEvent.send();
            log.debug("DataDog notification send", keyValue(DATADOG_EXPERIMENTID_KEY, event.getExperimentId()));
        } catch (StatsDClientException ex) {
            log.warn("Cannot send DataDog event: {}", ex);
        }
    }

    public DataDogNotification () {
        log.info("DataDog notification channel created");
    }

    DataDogNotification(StatsDClient statsDClient){
        this.statsDClient=statsDClient;
    }

    class DataDogEvent {
        private ChaosEvent chaosEvent;
        private final String eventPrefix = "Chaos Event ";

        DataDogEvent (ChaosEvent event) {
            this.chaosEvent = event;
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

        void send () {
            statsDClient.recordEvent(buildEvent(),generateTags());
        }

        String[] generateTags () {
            ArrayList<String> tags = new ArrayList<>();
            tags.add("ExperimentId:" + chaosEvent.getExperimentId());
            tags.add("Method:" + chaosEvent.getExperimentMethod());
            tags.add("Type:" + chaosEvent.getExperimentType().name());
            tags.add("Target:" + chaosEvent.getTargetContainer().getSimpleName());
            return tags.toArray(new String[0]);
        }
    }
}
