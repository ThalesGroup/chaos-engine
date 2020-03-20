/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.health.impl;

import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.AwsEC2Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("aws.ec2")
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
