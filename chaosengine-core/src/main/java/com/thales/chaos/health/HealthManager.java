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

package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class HealthManager {
    public static final Logger log = LoggerFactory.getLogger(HealthManager.class);
    @Autowired(required = false)
    private Collection<SystemHealth> systemHealth;

    @Autowired
    HealthManager () {
    }

    HealthManager (Collection<SystemHealth> systemHealth) {
        this.systemHealth = systemHealth;
    }

    SystemHealthState getHealth () {
        if (getSystemHealth() != null) {
            for (SystemHealth health : getSystemHealth()) {
                SystemHealthState healthCheck = health.getHealth();
                if (healthCheck == SystemHealthState.OK) {
                    log.debug("{} : {}", health.getClass().getSimpleName(), healthCheck);
                    continue;
                }
                log.error("{} : {}", health.getClass().getSimpleName(), healthCheck);
                return SystemHealthState.ERROR;
            }
            return SystemHealthState.OK;
        }
        return SystemHealthState.UNKNOWN;
    }

    Collection<SystemHealth> getSystemHealth () {
        return systemHealth;
    }
}
