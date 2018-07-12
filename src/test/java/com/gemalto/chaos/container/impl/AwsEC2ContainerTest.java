package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.impl.AwsPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Callable;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsEC2ContainerTest {
    private static final String KEY_NAME = randomUUID().toString();
    private static final String NAME = randomUUID().toString();
    private static final String INSTANCE_ID = randomUUID().toString();
    private AwsEC2Container awsEC2Container;
    @Mock
    private AwsPlatform awsPlatform;

    @Before
    public void setUp () {
        awsEC2Container = AwsEC2Container.builder()
                                         .keyName(KEY_NAME)
                                         .instanceId(INSTANCE_ID)
                                         .awsPlatform(awsPlatform)
                                         .name(NAME)
                                         .build();
    }

    @Test
    public void getPlatform () {
        assertEquals(awsPlatform, awsEC2Container.getPlatform());
    }

    @Test
    public void updateContainerHealthImpl () {
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            when(awsPlatform.checkHealth(any(String.class))).thenReturn(containerHealth);
            assertEquals(containerHealth, awsEC2Container.updateContainerHealthImpl(null));
        }
    }

    @Test
    public void stopContainer () {
        Callable<Void> healthCheckFunction = awsEC2Container.stopContainer();
        try {
            healthCheckFunction.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Mockito.verify(awsPlatform, times(1)).stopInstance(INSTANCE_ID);
        Mockito.verify(awsPlatform, times(1)).startInstance(INSTANCE_ID);
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s) [%s]", NAME, KEY_NAME, INSTANCE_ID);
        assertEquals(EXPECTED_NAME, awsEC2Container.getSimpleName());
    }
}