package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.health.enums.SystemHealthState;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryHealthTest {

    @Mock
    private CloudFoundryPlatform cloudFoundryPlatform;

    @Test
    public void getHealth() {
        CloudFoundryHealth cfh = new CloudFoundryHealth(cloudFoundryPlatform);

        Mockito.when(cloudFoundryPlatform.getApiStatus())
                .thenReturn(ApiStatus.OK)
                .thenReturn(ApiStatus.ERROR)
                .thenReturn(null)
                .thenThrow(new RuntimeException());

        assertEquals(SystemHealthState.OK, cfh.getHealth());
        assertEquals(SystemHealthState.ERROR, cfh.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, cfh.getHealth());
        assertEquals(SystemHealthState.UNKNOWN, cfh.getHealth());

    }
}