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
import com.thales.chaos.platform.impl.CloudFoundryPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "cf.organization" })
public class CloudFoundryHealth extends AbstractPlatformHealth {
    @Autowired
    private CloudFoundryPlatform cloudFoundryPlatform;

    CloudFoundryHealth (CloudFoundryPlatform cloudFoundryPlatform) {
        this();
        this.cloudFoundryPlatform = cloudFoundryPlatform;
    }

    @Autowired
    CloudFoundryHealth () {
        log.debug("Using CloudFoundry API check for health check.");
    }

    @Override
    Platform getPlatform () {
        return cloudFoundryPlatform;
    }
}
