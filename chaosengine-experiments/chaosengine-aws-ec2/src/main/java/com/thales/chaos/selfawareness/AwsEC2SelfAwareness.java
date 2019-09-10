/*
 *    Copyright (c) 2019 Thales Group
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

package com.thales.chaos.selfawareness;

import com.thales.chaos.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class AwsEC2SelfAwareness {
    private static final Logger log = LoggerFactory.getLogger(AwsEC2SelfAwareness.class);
    private String instanceId;
    private boolean initialized = false;
    @Value("${aws.callback.host}")
    private String awsCallbackHost;

    public boolean isMe (@NotNull String otherInstanceId) {
        if (!initialized) init();
        return (otherInstanceId.equals(instanceId));
    }

    private synchronized void init () {
        if (initialized) return;
        instanceId = fetchInstanceId();
        if (instanceId == null) {
            log.info("This does not appear to be running in AWS EC2");
        } else {
            log.info("Running in AWS EC2 as Instance: {}", instanceId);
        }
        initialized = true;
    }

    String fetchInstanceId () {
        return HttpUtils.curl(String.format("http://%s/latest/meta-data/instance-id", awsCallbackHost), true);
    }
}
