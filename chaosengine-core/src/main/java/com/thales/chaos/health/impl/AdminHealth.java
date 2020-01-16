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

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.health.SystemHealth;
import com.thales.chaos.health.enums.SystemHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(AdminManager.class)
public class AdminHealth implements SystemHealth {
    private static final Logger log = LoggerFactory.getLogger(AdminHealth.class);
    @Autowired
    private AdminManager adminManager;

    @Autowired
    AdminHealth () {
        log.debug("Using Administrative State for health check");
    }

    @Override
    public SystemHealthState getHealth () {
        boolean healthyState = AdminState.getHealthyStates().contains(adminManager.getAdminState());
        boolean longTimeInState = adminManager.getTimeInState().getSeconds() > 60 * 5;
        return (!healthyState) && longTimeInState ? SystemHealthState.ERROR : SystemHealthState.OK;
    }
}
