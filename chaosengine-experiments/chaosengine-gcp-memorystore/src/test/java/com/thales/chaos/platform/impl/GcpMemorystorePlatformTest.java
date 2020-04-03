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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.redis.v1.FailoverInstanceRequest;
import com.google.cloud.redis.v1.Instance;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpMemorystoreInstanceContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.services.impl.GcpCredentialsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class GcpMemorystorePlatformTest {
    private static final String PROJECT_NAME = "gcp-project";
    private static final String EMAIL = "user@example.com";
    @MockBean
    private CredentialsProvider credentialsProvider;
    @MockBean
    private ContainerManager containerManager;
    @Autowired
    private GcpMemorystorePlatform platform;
    @Autowired
    private GcpCredentialsMetadata gcpCredentialsMetadata;
    private Instance eligibleInstance;
    private Instance startingInstance;
    private GcpMemorystoreInstanceContainer eligibleContainer;
    private GcpMemorystoreInstanceContainer startingContainer;
    private Map<String, String> labels = new HashMap<>();

    @Before
    public void setUp () {
        labels.put("chaos", "yes");
        eligibleInstance = Instance.newBuilder()
                                   .putAllLabels(labels)
                                   .setState(Instance.State.READY)
                                   .setTier(Instance.Tier.STANDARD_HA)
                                   .build();
        startingInstance = Instance.newBuilder()
                                   .putAllLabels(labels)
                                   .setState(Instance.State.CREATING)
                                   .setTier(Instance.Tier.STANDARD_HA)
                                   .build();
        eligibleContainer = platform.createContainerFromInstance(eligibleInstance);
        startingContainer = platform.createContainerFromInstance(startingInstance);
    }

    @Test
    public void generateRoster () {
        Instance nonEligibleInstance = Instance.newBuilder()
                                               .putAllLabels(labels)
                                               .setState(Instance.State.READY)
                                               .setTier(Instance.Tier.BASIC)
                                               .build();
        Instance untaggedInstance = Instance.newBuilder()
                                            .setState(Instance.State.READY)
                                            .setTier(Instance.Tier.STANDARD_HA)
                                            .build();
        List<Instance> instanceList = List.of(eligibleInstance,
                startingInstance,
                nonEligibleInstance,
                untaggedInstance);
        doReturn(instanceList).when(platform).getInstances(any());
        GcpMemorystoreInstanceContainer untaggedContainer = platform.createContainerFromInstance(untaggedInstance);
        //No filter set
        assertThat(platform.generateRoster(), containsInAnyOrder(eligibleContainer, untaggedContainer));
        assertThat(platform.generateRoster().size(), is(2));
        platform.generateRoster();
        //Include filter set
        platform.setIncludeFilter(labels);
        assertThat(platform.generateRoster(), containsInAnyOrder(eligibleContainer));
        assertThat(platform.generateRoster().size(), is(1));
        platform.generateRoster();
        //Exclude filter set
        platform.setIncludeFilter(Collections.emptyMap());
        platform.setExcludeFilter(labels);
        assertThat(platform.generateRoster(), containsInAnyOrder(untaggedContainer));
        assertThat(platform.generateRoster().size(), is(1));
        platform.generateRoster();
    }

    @Test(expected = ChaosException.class)
    public void isContainerRecycled () {
        platform.isContainerRecycled(eligibleContainer);
    }

    @Test
    public void isContainerRunning () {
        doReturn(eligibleInstance).doReturn(startingInstance).doReturn(null).when(platform).getInstance(any());
        assertThat(platform.isContainerRunning(eligibleContainer), is(ContainerHealth.NORMAL));
        assertThat(platform.isContainerRunning(startingContainer), is(ContainerHealth.RUNNING_EXPERIMENT));
        assertThat(platform.isContainerRunning(eligibleContainer), is(ContainerHealth.DOES_NOT_EXIST));
    }

    @Test
    public void getApiStatus () {
        doReturn(null).doThrow(RuntimeException.class).when(platform).getInstances(any());
        assertThat(platform.getApiStatus(), is(ApiStatus.OK));
        assertThat(platform.getApiStatus(), is(ApiStatus.ERROR));
    }

    @Test
    public void getPlatformHealth () {
        List<Instance> instanceList = List.of(eligibleInstance);
        doReturn(instanceList).doReturn(List.of()).doThrow(RuntimeException.class).when(platform).getInstances(any());
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.OK));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.DEGRADED));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.FAILED));
    }

    @Test
    public void getPlatformLevel () {
        assertThat(platform.getPlatformLevel(), is(PlatformLevel.SAAS));
    }

    @Test
    public void failover () throws ExecutionException, InterruptedException {
        String opId = "id12345";
        FailoverInstanceRequest expectedFailoverInstanceRequest = FailoverInstanceRequest.newBuilder()
                                                                                         .
                                                                                                 setName(eligibleContainer
                                                                                                         .getName())
                                                                                         .setDataProtectionMode(
                                                                                                 FailoverInstanceRequest.DataProtectionMode.LIMITED_DATA_LOSS)
                                                                                         .build();
        ArgumentCaptor<FailoverInstanceRequest> captor = ArgumentCaptor.forClass(FailoverInstanceRequest.class);
        doReturn(opId).when(platform).executeFailover(captor.capture());
        assertThat(platform.failover(eligibleContainer), is(opId));
        assertThat(captor.getValue(), is(expectedFailoverInstanceRequest));
    }

    @Test
    public void forcedFailover () throws ExecutionException, InterruptedException {
        String opId = "id12345";
        FailoverInstanceRequest expectedFailoverInstanceRequest = FailoverInstanceRequest.newBuilder()
                                                                                         .setName(eligibleContainer.getName())
                                                                                         .setDataProtectionMode(
                                                                                                 FailoverInstanceRequest.DataProtectionMode.FORCE_DATA_LOSS)
                                                                                         .build();
        ArgumentCaptor<FailoverInstanceRequest> captor = ArgumentCaptor.forClass(FailoverInstanceRequest.class);
        doReturn(opId).when(platform).executeFailover(captor.capture());
        assertThat(platform.forcedFailover(eligibleContainer), is(opId));
        assertThat(captor.getValue(), is(expectedFailoverInstanceRequest));
    }

    @Configuration
    public static class GcpComputePlatformTestConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private CredentialsProvider credentialsProvider;

        @Bean
        public GcpMemorystorePlatform gcpComputePlatform () {
            return spy(new GcpMemorystorePlatform());
        }

        @Bean
        public GcpCredentialsMetadata gcpCredentialsMetadata () {
            return new GcpCredentialsMetadata(PROJECT_NAME);
        }
    }
}