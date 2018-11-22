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
        public static final String EVENT_PREFIX = "Chaos Event ";
        public static final String SOURCE_TYPE = "java";

        public static final String EXPERIMENT_ID = "ExperimentId:";
        public static final String METHOD = "Method:";
        public static final String TYPE = "Type:";
        public static final String TARGET = "Target:";

        DataDogEvent (ChaosEvent event) {
            this.chaosEvent = event;
        }

        Event buildEvent () {
            return Event.builder()
                        .withAggregationKey(chaosEvent.getExperimentId())
                        .withAlertType(mapLevel(chaosEvent.getNotificationLevel()))
                        .withTitle(EVENT_PREFIX + chaosEvent.getExperimentMethod())
                        .withText(chaosEvent.getMessage())
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

        void send () {
            statsDClient.recordEvent(buildEvent(),generateTags());
        }

        String[] generateTags () {
            ArrayList<String> tags = new ArrayList<>();
            tags.add(EXPERIMENT_ID + chaosEvent.getExperimentId());
            tags.add(METHOD + chaosEvent.getExperimentMethod());
            tags.add(TYPE + chaosEvent.getExperimentType().name());
            tags.add(TARGET + chaosEvent.getTargetContainer().getSimpleName());
            return tags.toArray(new String[0]);
        }
    }
}
