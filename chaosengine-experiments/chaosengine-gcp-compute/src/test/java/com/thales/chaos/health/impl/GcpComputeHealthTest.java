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

import com.thales.chaos.health.enums.SystemHealthState;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.impl.GcpComputePlatform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class GcpComputeHealthTest {
    @MockBean
    private GcpComputePlatform gcpComputePlatform;
    @Autowired
    private GcpComputeHealth gcpComputeHealth;

    @Test
    public void getHealth () {
        doReturn(ApiStatus.OK).doReturn(ApiStatus.ERROR)
                              .doReturn(null)
                              .doThrow(new RuntimeException())
                              .when(gcpComputePlatform)
                              .getApiStatus();
        assertEquals(SystemHealthState.OK, gcpComputeHealth.getHealth());
        assertEquals(SystemHealthState.ERROR, gcpComputeHealth.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, gcpComputeHealth.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, gcpComputeHealth.getHealth());
    }

    @Configuration
    public static class GcpComputeHealthTestConfiguration {
        @Autowired
        private GcpComputePlatform gcpComputePlatform;

        @Bean
        public GcpComputeHealth gcpComputeHealth () {
            return spy(new GcpComputeHealth());
        }
    }
}