package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.platform.enums.ApiStatus;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryPlatformTest {
    @Mock
    private CloudFoundryOperations cloudFoundryOperations;
    @Mock
    private ContainerManager containerManager;

    @Test
    public void getApiStatus () {
        CloudFoundryPlatform cfp = new CloudFoundryPlatform(cloudFoundryOperations, containerManager);
        Mockito.when(cloudFoundryOperations.applications()).thenThrow(Mockito.mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, cfp.getApiStatus());
    }
}