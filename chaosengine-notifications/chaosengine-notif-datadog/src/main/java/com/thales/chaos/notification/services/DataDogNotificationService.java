package com.thales.chaos.notification.services;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotificationService {
    private static final int STATSD_PORT = 8125;
    private static final String STATSD_HOST = "datadog";
    private static final String[] STATIC_TAGS = { "service:chaosengine" };

    @Bean
    StatsDClient statsDClient(){
        return new NonBlockingStatsDClient("", STATSD_HOST, STATSD_PORT, STATIC_TAGS);
    }
}
