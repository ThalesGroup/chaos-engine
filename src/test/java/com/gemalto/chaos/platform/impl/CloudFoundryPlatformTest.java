package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryPlatformTest {
    @Mock
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    @Mock
    private CloudFoundryOperations cloudFoundryOperations;
    @Mock
    private ContainerManager containerManager;
    @Mock
    private Applications applications;
    @Mock
    private CloudFoundryClient cloudFoundryClient;
    @Mock
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;
    private CloudFoundryPlatform cfp;

    @Before
    public void setUp () {
        cfp = CloudFoundryPlatform.getBuilder()
                                  .withCloudFoundryOperations(cloudFoundryOperations)
                                  .withCloudFoundryPlatformInfo(cloudFoundryPlatformInfo)
                                  .withCloudFoundrySelfAwareness(cloudFoundrySelfAwareness)
                                  .build();
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


}
