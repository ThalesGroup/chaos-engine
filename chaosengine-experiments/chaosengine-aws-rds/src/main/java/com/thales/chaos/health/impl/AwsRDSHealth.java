package com.thales.chaos.health.impl;

import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.AwsRDSPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@ConditionalOnProperty("aws.rds")
@Component
public class AwsRDSHealth extends AbstractPlatformHealth {
    @Autowired
    private AwsRDSPlatform awsRDSPlatform;

    @Override
    Platform getPlatform () {
        return awsRDSPlatform;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void postConstruct () {
        log.debug("Using AWS RDS API Status for Health Check endpoint");
    }
}
