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
import com.thales.chaos.platform.impl.AwsEC2Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsEC2HealthTest {
    @Mock
    private AwsEC2Platform platform;
    private AwsEC2Health health;

    @Before
    public void setUp () {
        health = new AwsEC2Health(platform);
    }

    @Test
    public void getHealth () {
        Mockito.when(platform.getApiStatus()).thenReturn(ApiStatus.OK).thenReturn(ApiStatus.ERROR).thenReturn(null).thenThrow(new RuntimeException());
        assertEquals(SystemHealthState.OK, health.getHealth());
        assertEquals(SystemHealthState.ERROR, health.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, health.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, health.getHealth());
    }

    @Test
    public void getPlatform () {
        assertEquals(platform, health.getPlatform());
    }
}