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

package com.thales.chaos.health.impl;

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.health.enums.SystemHealthState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;

import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdminHealthTest {
    @MockBean
    private AdminManager adminManager;
    @Autowired
    private AdminHealth adminHealth;

    @Test
    public void getHealth () {
        Mockito.doReturn(AdminState.STARTED).when(adminManager).getAdminState();
        doReturn(Duration.ZERO).when(adminManager).getTimeInState();
        Assert.assertEquals(SystemHealthState.OK, adminHealth.getHealth());
    }

    @Configuration
    static class TestConfig {
        @Bean
        AdminHealth adminHealth () {
            return Mockito.spy(new AdminHealth());
        }
    }
}