package com.gemalto.chaos.notification.services;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "dd_enable_events", havingValue = "true")
public class DataDogNotificationService {
    private final int statsdPort = 8125;
    private final String statsdHost = "datadog";
    private final String staticTags[] = {"service:chaosengine"};

    @Bean
    StatsDClient statsDClient(){
        return new NonBlockingStatsDClient("", statsdHost, statsdPort, staticTags);
    }
}
