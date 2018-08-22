package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "aws.ec2.accessKeyId", "aws.ec2.secretAccessKey" })
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
