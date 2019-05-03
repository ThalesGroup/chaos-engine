package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.health.enums.SystemHealthState;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;
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
        Mockito.when(platform.getApiStatus())
               .thenReturn(ApiStatus.OK)
               .thenReturn(ApiStatus.ERROR)
               .thenReturn(null)
               .thenThrow(new RuntimeException());
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