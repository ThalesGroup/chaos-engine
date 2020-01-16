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

import com.thales.chaos.constants.AwsEC2Constants;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.impl.AwsEC2Platform;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AwsEC2ContainerTest {
    private static final String KEY_NAME = randomUUID().toString();
    private static final String NAME = randomUUID().toString();
    private static final String INSTANCE_ID = randomUUID().toString();
    private static final String GROUP_IDENTIFIER = randomUUID().toString();
    private AwsEC2Container awsEC2Container;
    @MockBean
    private AwsEC2Platform awsEC2Platform;
    @Mock
    private Experiment experiment;

    @Before
    public void setUp () {
        awsEC2Container = spy(AwsEC2Container.builder()
                                             .keyName(KEY_NAME)
                                             .instanceId(INSTANCE_ID)
                                             .awsEC2Platform(awsEC2Platform)
                                             .name(NAME)
                                             .groupIdentifier(GROUP_IDENTIFIER)
                                             .build());
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getCheckContainerHealth();
            return null;
        }).when(experiment).setCheckContainerHealth(any());
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getSelfHealingMethod();
            return null;
        }).when(experiment).setSelfHealingMethod(any());
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getFinalizeMethod();
            return null;
        }).when(experiment).setFinalizeMethod(any());
    }

    @Test
    public void getAggegationIdentifier () {
        Assert.assertEquals(GROUP_IDENTIFIER, AwsEC2Container.builder()
                                                             .instanceId(INSTANCE_ID)
                                                             .name(NAME)
                                                             .groupIdentifier(GROUP_IDENTIFIER)
                                                             .build()
                                                             .getAggregationIdentifier());
        Assert.assertEquals(NAME, AwsEC2Container.builder()
                                                 .instanceId(INSTANCE_ID)
                                                 .name(NAME)
                                                 .groupIdentifier(AwsEC2Constants.NO_GROUPING_IDENTIFIER)
                                                 .build()
                                                 .getAggregationIdentifier());
        Assert.assertEquals(INSTANCE_ID, AwsEC2Container.builder()
                                                        .instanceId(INSTANCE_ID)
                                                        .name("")
                                                        .groupIdentifier(AwsEC2Constants.NO_GROUPING_IDENTIFIER)
                                                        .build()
                                                        .getAggregationIdentifier());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsEC2Platform, awsEC2Container.getPlatform());
    }

    @Test
    public void supportsShellBasedExperiments () {
        assertFalse(awsEC2Container.supportsShellBasedExperiments());
    }

    @Test
    public void updateContainerHealthImpl () {
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            when(awsEC2Platform.checkHealth(any(String.class))).thenReturn(containerHealth);
            assertEquals(containerHealth, awsEC2Container.updateContainerHealthImpl(null));
        }
    }

    @Test
    public void stopContainer () throws Exception {
        awsEC2Container.stopContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        verify(awsEC2Platform, times(1)).stopInstance(INSTANCE_ID);
        verify(awsEC2Platform, times(0)).startInstance(INSTANCE_ID);
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).startInstance(INSTANCE_ID);
        verify(awsEC2Platform, times(0)).checkHealth(INSTANCE_ID);
        experiment.getCheckContainerHealth().call();
        verify(awsEC2Platform, times(1)).checkHealth(INSTANCE_ID);
    }

    @Test
    public void restartContainer () throws Exception {
        awsEC2Container.restartContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        verify(awsEC2Platform, times(1)).restartInstance(INSTANCE_ID);
        verify(awsEC2Platform, times(0)).startInstance(INSTANCE_ID);
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).startInstance(INSTANCE_ID);
        await().atMost(4, TimeUnit.MINUTES).until(() -> ContainerHealth.RUNNING_EXPERIMENT == experiment.getCheckContainerHealth().call());
        experiment.getCheckContainerHealth().call();
    }

    @Test
    public void terminateASGContainer () throws Exception {
        doReturn(true).when(awsEC2Container).isNativeAwsAutoscaling();
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        awsEC2Container.terminateASGContainer(experiment);
        // Case 1 - Verify that the Terminate method was actually called
        verify(awsEC2Platform, times(1)).terminateInstance(INSTANCE_ID);
        // Case 2 - Verify that Check Container Health and Self Healing methods were set.
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        // Case 3 - Verify self healing method calls Trigger Autoscaling Unhealthy
        verify(awsEC2Platform, never()).triggerAutoscalingUnhealthy(INSTANCE_ID);
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).triggerAutoscalingUnhealthy(INSTANCE_ID);
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).triggerAutoscalingUnhealthy(INSTANCE_ID);
        //Healthcheck is triggered by triggerAutoscalingUnhealthy method
        doReturn(false).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        verify(awsEC2Platform, never()).isContainerTerminated(INSTANCE_ID);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(awsEC2Platform, times(1)).isContainerTerminated(INSTANCE_ID);
    }

    @Test
    public void terminateNonASGContainer () {
        doReturn(false).when(awsEC2Container).isNativeAwsAutoscaling();
        try {
            awsEC2Container.terminateASGContainer(experiment);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            assertEquals("Thrown exception should be a Chaos Exception for catch purposes", e.getClass(), ChaosException.class);
        }
        verify(awsEC2Platform, never().description("Should not have terminated any instances that aren't in an Autoscaling Group"))
                .terminateInstance(any());
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s) [%s]", NAME, KEY_NAME, INSTANCE_ID);
        assertEquals(EXPECTED_NAME, awsEC2Container.getSimpleName());
    }

    @Test
    public void removeSecurityGroupsSingleNetworkInterface () throws Exception {
        String chaosSecurityGroupId = "sg-ch@0$";
        Map<String, Set<String>> map = Map.of("eni-123456", Set.of("sg-123456"));
        doReturn(map).when(awsEC2Platform).getNetworkInterfaceToSecurityGroupsMap(INSTANCE_ID);
        doReturn(chaosSecurityGroupId).when(awsEC2Platform).getChaosSecurityGroupForInstance(INSTANCE_ID);
        // Test main experiment
        awsEC2Container.removeSecurityGroups(experiment);
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-123456", Set.of(chaosSecurityGroupId));
        // Test self healing
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-123456", Set.of("sg-123456"));
        // Test health check method
        experiment.getCheckContainerHealth().call();
        verify(awsEC2Platform, times(1)).verifySecurityGroupIdsOfNetworkInterfaceMap(INSTANCE_ID, map);
    }

    @Test
    public void removeSecurityGroupsMultipleNetworkInterfaces () throws Exception {
        String chaosSecurityGroupId = "sg-ch@0$";
        Map<String, Set<String>> map = Map.of("eni-123456", Set.of("sg-123456"), "eni-987654", Set.of("sg-987654", "sg-314159"));
        doReturn(map).when(awsEC2Platform).getNetworkInterfaceToSecurityGroupsMap(INSTANCE_ID);
        doReturn(chaosSecurityGroupId).when(awsEC2Platform).getChaosSecurityGroupForInstance(INSTANCE_ID);
        // Test main experiment
        awsEC2Container.removeSecurityGroups(experiment);
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-123456", Set.of(chaosSecurityGroupId));
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-987654", Set.of(chaosSecurityGroupId));
        // Test self healing
        experiment.getSelfHealingMethod().run();
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-123456", Set.of("sg-123456"));
        verify(awsEC2Platform, times(1)).setSecurityGroupIds("eni-987654", Set.of("sg-987654", "sg-314159"));
        // Test health check method
        experiment.getCheckContainerHealth().call();
        verify(awsEC2Platform, times(1)).verifySecurityGroupIdsOfNetworkInterfaceMap(INSTANCE_ID, map);
    }

    @Test
    public void getDataDogIdentifier () {
        TestCase.assertEquals(DataDogIdentifier.dataDogIdentifier().withValue(INSTANCE_ID).withKey("host"), awsEC2Container.getDataDogIdentifier());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingWrapperFramework () {
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        Callable<ContainerHealth> callable = mock(Callable.class);
        assertNotEquals(callable, awsEC2Container.autoscalingHealthcheckWrapper(callable));
        doReturn(false).when(awsEC2Container).isMemberOfScaledGroup();
        assertEquals(callable, awsEC2Container.autoscalingHealthcheckWrapper(callable));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingWrapperScaledAndTerminated () throws Exception {
        Callable<ContainerHealth> callable = mock(Callable.class);
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(callable).call();
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        doReturn(true).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        assertEquals(ContainerHealth.NORMAL, awsEC2Container.autoscalingHealthcheckWrapper(callable).call());
        verify(awsEC2Platform, times(1)).isContainerTerminated(INSTANCE_ID);
        verify(callable, never()).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingWrapperScaledNotTerminated () throws Exception {
        Callable<ContainerHealth> callable = mock(Callable.class);
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(callable).call();
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        doReturn(false).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Container.autoscalingHealthcheckWrapper(callable).call());
        verify(awsEC2Platform, times(1)).isContainerTerminated(INSTANCE_ID);
        verify(callable, atLeastOnce()).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingWrapperNotScaled () throws Exception {
        Callable<ContainerHealth> callable = mock(Callable.class);
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(callable).call();
        doReturn(false).when(awsEC2Container).isMemberOfScaledGroup();
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Container.autoscalingHealthcheckWrapper(callable).call());
        verify(awsEC2Platform, never()).isContainerTerminated(INSTANCE_ID);
        verify(callable, atLeastOnce()).call();
    }

    @Test
    public void isMemberOfScaledGroup () {
        assertTrue(awsEC2Container.isMemberOfScaledGroup());
        ReflectionTestUtils.setField(awsEC2Container, "groupIdentifier", AwsEC2Constants.NO_GROUPING_IDENTIFIER);
        assertFalse(awsEC2Container.isMemberOfScaledGroup());
    }

    @Test
    public void autoscalingSelfHealingWrapper () {
        doReturn(true).when(awsEC2Container).isNativeAwsAutoscaling();
        Runnable baseMethod = spy(Runnable.class);
        Runnable callable = awsEC2Container.autoscalingSelfHealingWrapper(baseMethod);
        callable.run();
        /*
        On the first call, it should use the Autoscaling method, and not touch the base method.
         */
        verify(awsEC2Platform, atLeastOnce()).triggerAutoscalingUnhealthy(anyString());
        verify(baseMethod, never()).run();
        reset(baseMethod, awsEC2Platform);
        callable.run();
        /*
        On the second call, it should not use the Autoscaling method, and use the base method.
         */
        verify(awsEC2Platform, never()).triggerAutoscalingUnhealthy(anyString());
        verify(baseMethod, atLeastOnce()).run();
        reset(baseMethod, awsEC2Platform);
        /*
        If a Runtime exception occurs with the autoscaling method, it should use the base method on subsequent calls.
         */
        callable = awsEC2Container.autoscalingSelfHealingWrapper(baseMethod);
        doThrow(new RuntimeException()).when(awsEC2Platform).triggerAutoscalingUnhealthy(anyString());
        try {
            callable.run();
            fail("We expect an exception to be thrown for this test.");
        } catch (RuntimeException expected) {
        }
        callable.run();
        verify(baseMethod, times(1)).run();
    }

    @Test
    public void autoscalingSelfHealingWrapperOnNativeInstance () {
        doReturn(false).when(awsEC2Container).isNativeAwsAutoscaling();
        Runnable baseMethod = () -> {
        };
        assertSame(baseMethod, awsEC2Container.autoscalingSelfHealingWrapper(baseMethod));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingHealthCheckWrapperNativeAutoscaling () throws Exception {
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        doReturn(true).when(awsEC2Container).isNativeAwsAutoscaling();
        Callable<ContainerHealth> baseMethod = spy(Callable.class);
        Callable<ContainerHealth> callable;
        /*
        Case 1: Not terminated. Expect the base method to be called.
         */
        doReturn(false).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        callable = awsEC2Container.autoscalingHealthcheckWrapper(baseMethod);
        callable.call();
        verify(baseMethod, atLeastOnce()).call();
        assertNotSame(callable, baseMethod);
        reset(baseMethod);
        /*
        Case 2/3: Terminated, 2/Not At Capacity, 3/At Capacity
         */
        doReturn(true).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        doReturn(true, false).when(awsEC2Platform).isAutoscalingGroupAtDesiredInstances(GROUP_IDENTIFIER);
        callable = awsEC2Container.autoscalingHealthcheckWrapper(baseMethod);
        assertEquals(ContainerHealth.NORMAL, callable.call());
        verify(baseMethod, never()).call();
        callable = awsEC2Container.autoscalingHealthcheckWrapper(baseMethod);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, callable.call());
        verify(baseMethod, never()).call();
    }

    @Test
    public void autoscalingHealthCheckWrapperThirdPartyAutoscaling () throws Exception {
        doReturn(true).when(awsEC2Container).isMemberOfScaledGroup();
        doReturn(false).when(awsEC2Container).isNativeAwsAutoscaling();
        doReturn(true).when(awsEC2Platform).isContainerTerminated(INSTANCE_ID);
        assertEquals(ContainerHealth.NORMAL, awsEC2Container.autoscalingHealthcheckWrapper(() -> null).call());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingHealthCheckWrapperNoAutoscaling () {
        doReturn(false).when(awsEC2Container).isMemberOfScaledGroup();
        Callable<ContainerHealth> baseMethod = spy(Callable.class);
        Callable<ContainerHealth> callable;
        callable = awsEC2Container.autoscalingHealthcheckWrapper(baseMethod);
        assertSame(baseMethod, callable);
    }

    @Test
    public void dataDogTags () {
        Map<String, String> baseContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        awsEC2Container.setMappedDiagnosticContext();
        Map<String, String> modifiedContextMap = MDC.getCopyOfContextMap();
        awsEC2Container.clearMappedDiagnosticContext();
        Map<String, String> finalContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, INSTANCE_ID);
        expectedTags.putAll(baseContextMap);
        assertEquals(baseContextMap, finalContextMap);
        assertEquals(expectedTags, modifiedContextMap);
    }

    @Test
    public void getRoutableAddress () {
        doReturn(null, "private").when(awsEC2Container).getPrivateAddress();
        doReturn("public").when(awsEC2Container).getPublicAddress();
        doReturn(true, false).when(awsEC2Platform).isAddressRoutable("private");
        assertEquals("Null private address, should revert to public", "public", awsEC2Container.getRoutableAddress());
        verify(awsEC2Platform, never()).isAddressRoutable(any());
        assertEquals("Routable private address, should be private", "private", awsEC2Container.getRoutableAddress());
        assertEquals("Unroutable private address, should be private", "public", awsEC2Container.getRoutableAddress());
    }

    @Test
    public void isStarted () {
        for (boolean result : List.of(true, false, true, false, true, false)) {
            doReturn(result).when(awsEC2Platform).isStarted(awsEC2Container);
            assertEquals(result, awsEC2Container.isStarted());
        }
    }

    @Test
    public void canExperiment () {
        doReturn(0D, 0D, 0D, 0.2D).when(awsEC2Platform).getDestructionProbability();
        doReturn(true).when(awsEC2Platform).isStarted(awsEC2Container);
        do {
            verify(awsEC2Platform, never()).isStarted(awsEC2Container);
        } while (!awsEC2Container.canExperiment());
        verify(awsEC2Platform, times(1)).isStarted(awsEC2Container);
    }
}