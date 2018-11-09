package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
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
    @Spy
    private Experiment experiment = new Experiment() {
    };

    @Before
    public void setUp () {
        awsEC2Container = Mockito.spy(AwsEC2Container.builder()
                                                     .keyName(KEY_NAME)
                                                     .instanceId(INSTANCE_ID)
                                                     .awsEC2Platform(awsEC2Platform)
                                                     .name(NAME)
                                                     .groupIdentifier(GROUP_IDENTIFIER)
                                                     .build());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsEC2Platform, awsEC2Container.getPlatform());
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
        Mockito.verify(awsEC2Platform, times(1)).stopInstance(INSTANCE_ID);
        Mockito.verify(awsEC2Platform, times(0)).startInstance(INSTANCE_ID);
        experiment.getSelfHealingMethod().call();
        Mockito.verify(awsEC2Platform, times(1)).startInstance(INSTANCE_ID);
        Mockito.verify(awsEC2Platform, times(0)).checkHealth(INSTANCE_ID);
        experiment.getCheckContainerHealth().call();
        Mockito.verify(awsEC2Platform, times(1)).checkHealth(INSTANCE_ID);
    }

    @Test
    public void restartContainer () throws Exception {
        awsEC2Container.restartContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(awsEC2Platform, times(1)).restartInstance(INSTANCE_ID);
        Mockito.verify(awsEC2Platform, times(0)).startInstance(INSTANCE_ID);
        experiment.getSelfHealingMethod().call();
        Mockito.verify(awsEC2Platform, times(1)).startInstance(INSTANCE_ID);
        await().atMost(4, TimeUnit.MINUTES)
               .until(() -> ContainerHealth.RUNNING_EXPERIMENT == experiment.getCheckContainerHealth().call());
        experiment.getCheckContainerHealth().call();
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s) [%s]", NAME, KEY_NAME, INSTANCE_ID);
        assertEquals(EXPECTED_NAME, awsEC2Container.getSimpleName());
    }

    @Test
    public void removeSecurityGroups () throws Exception {
        String chaosSecurityGroupId = UUID.randomUUID().toString();
        List<String> configuredSecurityGroupId = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID()
                                                                                                 .toString());
        doReturn(configuredSecurityGroupId).when(awsEC2Platform).getSecurityGroupIds(INSTANCE_ID);
        doReturn(chaosSecurityGroupId).when(awsEC2Platform).getChaosSecurityGroupId();
        Mockito.verify(awsEC2Platform, times(0))
               .setSecurityGroupIds(INSTANCE_ID, Collections.singletonList(chaosSecurityGroupId));
        verify(experiment, times(0)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(0)).setSelfHealingMethod(ArgumentMatchers.any());
        awsEC2Container.removeSecurityGroups(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(awsEC2Platform, times(1))
               .setSecurityGroupIds(INSTANCE_ID, Collections.singletonList(chaosSecurityGroupId));
        Mockito.verify(awsEC2Platform, times(0)).verifySecurityGroupIds(INSTANCE_ID, configuredSecurityGroupId);
        experiment.getCheckContainerHealth().call();
        Mockito.verify(awsEC2Platform, times(1)).verifySecurityGroupIds(INSTANCE_ID, configuredSecurityGroupId);
        Mockito.verify(awsEC2Platform, times(0)).setSecurityGroupIds(INSTANCE_ID, configuredSecurityGroupId);
        experiment.getSelfHealingMethod().call();
        Mockito.verify(awsEC2Platform, times(1)).setSecurityGroupIds(INSTANCE_ID, configuredSecurityGroupId);
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier()
                                      .withValue(INSTANCE_ID)
                                      .withKey("Host"), awsEC2Container.getDataDogIdentifier());
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
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Container.autoscalingHealthcheckWrapper(callable)
                                                                        .call());
        verify(awsEC2Platform, times(1)).isContainerTerminated(INSTANCE_ID);
        verify(callable, atLeastOnce()).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void autoscalingWrapperNotScaled () throws Exception {
        Callable<ContainerHealth> callable = mock(Callable.class);
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(callable).call();
        doReturn(false).when(awsEC2Container).isMemberOfScaledGroup();
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Container.autoscalingHealthcheckWrapper(callable)
                                                                        .call());
        verify(awsEC2Platform, never()).isContainerTerminated(INSTANCE_ID);
        verify(callable, atLeastOnce()).call();
    }

    @Test
    public void isMemberOfScaledGroup () {
        assertTrue(awsEC2Container.isMemberOfScaledGroup());
        ReflectionTestUtils.setField(awsEC2Container, "groupIdentifier", AwsEC2Constants.NO_GROUPING_IDENTIFIER);
        assertFalse(awsEC2Container.isMemberOfScaledGroup());
    }
}