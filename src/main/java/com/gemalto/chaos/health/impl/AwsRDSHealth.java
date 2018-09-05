package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@ConditionalOnProperty({ "aws.rds.accessKeyId", "aws.rds.secretAccessKey" })
@Component
public class AwsRDSHealth extends AbstractPlatformHealth {
    @Autowired
    private AwsRDSPlatform awsRDSPlatform;

    @Override
    Platform getPlatform () {
        return awsRDSPlatform;
    }

    @PostConstruct
    private void postConstruct () {
        log.debug("Using AWS RDS API Status for Health Check endpoint");
    }
}
