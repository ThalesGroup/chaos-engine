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

import com.thales.chaos.health.SystemHealth;
import com.thales.chaos.health.enums.SystemHealthState;
import com.thales.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPlatformHealth implements SystemHealth {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public SystemHealthState getHealth () {
        try {
            switch (getPlatform().getApiStatus()) {
                case OK:
                    return SystemHealthState.OK;
                case ERROR:
                    return SystemHealthState.ERROR;
                default:
                    return SystemHealthState.UNKNOWN;
            }
        } catch (RuntimeException e) {
            log.error("Could not resolve API Status. Returning error", e);
            return SystemHealthState.UNKNOWN;
        }
    }

    abstract Platform getPlatform ();
}