package com.gemalto.chaos.notification.impl;


import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationMethods;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({"datadog_apikey"})
public class DataDogNotification implements NotificationMethods {

    @Value("${datadog_apikey")
    private String apikey;


    @Override
    public void logEvent(ChaosEvent event) {
        // TODO: Implement Datadog logging

    }
}
