package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.impl.AwsPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AwsEC2ContainerTest {
    private static final String KEY_NAME = randomUUID().toString();
    private static final String NAME = randomUUID().toString();
    private static final String INSTANCE_ID = randomUUID().toString();
    private AwsEC2Container awsEC2Container;
    @MockBean
    private AwsPlatform awsPlatform;
    @Spy
    private Attack attack = new Attack() {
    };

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
        awsEC2Container.stopContainer(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(awsPlatform, times(1)).stopInstance(INSTANCE_ID);
    }

    @Test
    public void restartContainer () {
        awsEC2Container.restartContainer(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(awsPlatform, times(1)).restartInstance(INSTANCE_ID);
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s) [%s]", NAME, KEY_NAME, INSTANCE_ID);
        assertEquals(EXPECTED_NAME, awsEC2Container.getSimpleName());
    }
}