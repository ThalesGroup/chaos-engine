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

package com.thales.chaos.container.impl;

import com.google.cloud.redis.v1.FailoverInstanceRequest;
import com.google.cloud.redis.v1.Instance;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.impl.GenericContainerExperiment;
import com.thales.chaos.platform.impl.GcpMemorystorePlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class GcpMemorystoreInstanceContainerTest {
    private static String DISPLAY_NAME = "Display name";
    private static String INSTANCE_ID = "redis-instance-test";
    private static String LOCATION_ID = "us-central1";
    private static String NAME = "projects/gemalto-cspeng/locations/" + LOCATION_ID + "/instances/" + INSTANCE_ID;
    private static String HOST = "localhost";
    private static String AGGREGATION_IDENTIFIER = INSTANCE_ID + "-" + LOCATION_ID;
    private static int PORT = 6379;
    private GcpMemorystoreInstanceContainer container;
    @Mock
    private GcpMemorystorePlatform platform;
    private Experiment experiment;
    private Instance instance;

    @Before
    public void setUp () {
        instance = Instance.newBuilder()
                           .setDisplayName(DISPLAY_NAME)
                           .setName(NAME)
                           .setLocationId(LOCATION_ID)
                           .setHost(HOST)
                           .setPort(PORT)
                           .setState(Instance.State.READY)
                           .build();
        container = spy(GcpMemorystoreInstanceContainer.builder()
                                                       .fromInstance(instance)
                                                       .withPlatform(platform)
                                                       .build());
        experiment = spy(GenericContainerExperiment.builder().withContainer(container).build());
    }

    @Test
    public void forceFailover () throws ExecutionException, InterruptedException {
        container.forceFailover(experiment);
        verify(platform).failover(container, FailoverInstanceRequest.DataProtectionMode.FORCE_DATA_LOSS);
        verify(experiment, Mockito.never()).setSelfHealingMethod(any());
        verify(experiment).setCheckContainerHealth(any());
    }

    @Test
    public void failover () throws ExecutionException, InterruptedException {
        container.failover(experiment);
        verify(platform).failover(container, FailoverInstanceRequest.DataProtectionMode.LIMITED_DATA_LOSS);
        verify(experiment, Mockito.never()).setSelfHealingMethod(any());
        verify(experiment).setCheckContainerHealth(any());
    }

    @Test
    public void buildFromInstance () {
        GcpMemorystoreInstanceContainer gcpMemorystoreInstanceContainer = GcpMemorystoreInstanceContainer.builder()
                                                                                                         .fromInstance(
                                                                                                                 instance)
                                                                                                         .withPlatform(
                                                                                                                 platform)
                                                                                                         .build();
        assertThat(gcpMemorystoreInstanceContainer.getDisplayName(), equalTo(DISPLAY_NAME));
        assertThat(gcpMemorystoreInstanceContainer.getName(), equalTo(NAME));
        assertThat(gcpMemorystoreInstanceContainer.getHost(), equalTo(HOST));
        assertThat(gcpMemorystoreInstanceContainer.getPort(), equalTo(PORT));
        assertThat(gcpMemorystoreInstanceContainer.getPlatform(), equalTo(platform));
        assertThat(gcpMemorystoreInstanceContainer.getAggregationIdentifier(), equalTo(AGGREGATION_IDENTIFIER));
    }

    @Test
    public void compareUniqueIdentifierInner () {
        assertTrue(container.compareUniqueIdentifierInner(AGGREGATION_IDENTIFIER));
        assertFalse(container.compareUniqueIdentifierInner(INSTANCE_ID + "-us-central2"));
    }

    @Test
    public void getDataDogIdentifier () {
        assertThat(container.getDataDogIdentifier().getValue(), equalTo(INSTANCE_ID));
    }

    @Test
    public void updateContainerHealthImpl () {
        container.updateContainerHealthImpl(null);
        verify(platform).isContainerRunning(container);
    }
}