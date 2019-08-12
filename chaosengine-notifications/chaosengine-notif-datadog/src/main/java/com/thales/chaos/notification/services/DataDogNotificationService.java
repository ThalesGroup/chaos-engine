package com.thales.chaos.notification.services;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "datadog")
@ConditionalOnProperty(name = "datadog.enableEvents", havingValue = "true")
public class DataDogNotificationService {
    private int statsdPort = 8125;
    private String statsdHost = "datadog";
    private static final String[] STATIC_TAGS = { "service:chaosengine" };

    public void setStatsdPort (int statsdPort) {
        this.statsdPort = statsdPort;
    }

    public void setStatsdHost (String statsdHost) {
        this.statsdHost = statsdHost;
    }

    @Bean
    StatsDClient statsDClient(){
        return new NonBlockingStatsDClient("", statsdHost, statsdPort, STATIC_TAGS);
    }
}
