package com.thales.chaos.health.impl;

import com.thales.chaos.health.enums.SystemHealthState;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryHealthTest {
    @Mock
    private CloudFoundryApplicationPlatform platform;
    private CloudFoundryHealth health;

    @Before
    public void setUp () {
        health = new CloudFoundryHealth(platform);
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