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

package com.thales.chaos.platform.impl;

import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.auth.oauth2.GoogleCredentials;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.services.impl.GcpCredentialsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class GcpSqlPlatformTest {
    private static final String PROJECT_NAME = "gcp-project";
    private static final String EMAIL = "user@example.com";
    @MockBean
    private GoogleCredentials googleCredentials;
    @MockBean
    private ContainerManager containerManager;
    @Autowired
    private GcpSqlPlatform platform;
    @Autowired
    private GcpCredentialsMetadata gcpCredentialsMetadata;

    @Before
    public void setUp () {
    }

    @Test
    public void getApiStatus () throws IOException, GeneralSecurityException {
        doReturn(null).doThrow(RuntimeException.class).when(platform).getInstances();
        assertThat(platform.getApiStatus(), is(ApiStatus.OK));
        assertThat(platform.getApiStatus(), is(ApiStatus.ERROR));
    }

    @Test
    public void getPlatformHealth () throws IOException, GeneralSecurityException {
        List<DatabaseInstance> instanceList = List.of(new DatabaseInstance());
        doReturn(instanceList).doReturn(List.of()).doThrow(RuntimeException.class).when(platform).getInstances();
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.OK));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.DEGRADED));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.FAILED));
    }

    @Test
    public void getPlatformLevel () {
        assertThat(platform.getPlatformLevel(), is(PlatformLevel.IAAS));
    }

    @Configuration
    public static class GcpSqlPlatformTestConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private GoogleCredentials googleCredentials;

        @Bean
        public GcpSqlPlatform gcpSqlPlatform () {
            return spy(new GcpSqlPlatform());
        }

        @Bean
        public GcpCredentialsMetadata gcpCredentialsMetadata () {
            return new GcpCredentialsMetadata(PROJECT_NAME, EMAIL);
        }
    }
}