package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.platform.enums.ApiStatus;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryPlatformTest {
    @Mock
    private CloudFoundryOperations cloudFoundryOperations;
    @Mock
    private ContainerManager containerManager;
    @Mock
    private Applications applications;
    private CloudFoundryPlatform cfp;

    @Before
    public void setUp () {
        cfp = new CloudFoundryPlatform(cloudFoundryOperations, containerManager);
    }

    @Test
    public void getApiStatusError () {
        when(cloudFoundryOperations.applications()).thenThrow(mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, cfp.getApiStatus());
    }

    @Test
    public void getApiStatusSuccess () {
        when(cloudFoundryOperations.applications()).thenReturn(applications);
        assertEquals(ApiStatus.OK, cfp.getApiStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void restartInstanceTest () {
        Mono<Void> monoVoid = mock(Mono.class);
        when(cloudFoundryOperations.applications()).thenReturn(applications);
        when(applications.restartInstance(null)).thenAnswer((Answer<Mono<Void>>) invocationOnMock -> monoVoid);
        cfp.restartInstance(null);
        verify(applications, times(1)).restartInstance(null);
        verify(monoVoid, times(1)).block();
    }
}