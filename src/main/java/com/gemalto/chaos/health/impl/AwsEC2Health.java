package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "aws.ec2.accessKeyId", "aws.ec2.secretAccessKey" })
public class AwsEC2Health extends AbstractPlatformHealth {
    @Autowired
    private AwsEC2Platform awsEC2Platform;

    AwsEC2Health (AwsEC2Platform awsEC2Platform) {
        this();
        this.awsEC2Platform = awsEC2Platform;
    }

    @Autowired
    AwsEC2Health () {
        log.debug("Using AWS EC2 Health Check for System Health verification");
    }

    @Override
    Platform getPlatform () {
        return awsEC2Platform;
    }
}
