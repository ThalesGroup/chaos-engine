package com.gemalto.chaos.notification;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({"datadog.apikey"})
public class DataDogNotification implements NotificationMethods {

    private String apikey;

    private static final Logger log = LoggerFactory.getLogger(DataDogNotification.class);

    public DataDogNotification(@Value("datadog.apikey") String apikey) {
        this.apikey = apikey;
    }

    @Override
    public void logEvent(String event) {
        log.info(event);
    }
}
