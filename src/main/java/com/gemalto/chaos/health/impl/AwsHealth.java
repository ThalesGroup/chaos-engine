package com.gemalto.chaos.health.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(AmazonEC2.class)
public class AwsHealth extends AbstractPlatformHealth {
    @Autowired
    private AwsPlatform awsPlatform;

    AwsHealth (AwsPlatform awsPlatform) {
        this();
        this.awsPlatform = awsPlatform;
    }

    @Autowired
    AwsHealth () {
        log.debug("Using AWS Health Check for System Health verification");
    }

    @Override
    Platform getPlatform () {
        return awsPlatform;
    }
}
