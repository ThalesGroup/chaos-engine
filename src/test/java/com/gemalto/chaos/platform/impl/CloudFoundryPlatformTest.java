package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;
import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STOPPED;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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
    private CloudFoundryPlatform cfp;

    @Before
    public void setUp () {
        cfp = new CloudFoundryPlatform(cloudFoundryPlatformInfo, cloudFoundryOperations, containerManager);
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
    public void getRoster () {
        String APPLICATION_NAME = randomUUID().toString();
        String APPLICATION_ID = randomUUID().toString();
        Integer INSTANCES = 2;
        CloudFoundryContainer EXPECTED_CONTAINER_1 = CloudFoundryContainer.builder()
                                                                          .applicationId(APPLICATION_ID)
                                                                          .fateManager(null)
                                                                          .instance(0)
                                                                          .platform(cfp)
                                                                          .name(APPLICATION_NAME)
                                                                          .build();
        CloudFoundryContainer EXPECTED_CONTAINER_2 = CloudFoundryContainer.builder()
                                                                          .applicationId(APPLICATION_ID)
                                                                          .fateManager(null)
                                                                          .instance(1)
                                                                          .platform(cfp)
                                                                          .name(APPLICATION_NAME)
                                                                          .build();
        ApplicationSummary.Builder builder = ApplicationSummary.builder()
                                                               .diskQuota(0)
                                                               .instances(INSTANCES)
                                                               .id(APPLICATION_ID)
                                                               .name(APPLICATION_NAME)
                                                               .addAllUrls(Collections.EMPTY_SET)
                                                               .runningInstances(INSTANCES)
                                                               .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                                               .memoryLimit(0);
        ApplicationSummary applicationSummary = builder.build();
        ApplicationSummary stoppedApplicationSummary = builder.requestedState(CLOUDFOUNDRY_APPLICATION_STOPPED).build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary, stoppedApplicationSummary);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        when(containerManager.getOrCreatePersistentContainer(any(Container.class))).thenAnswer((Answer<Container>) invocation -> (Container) invocation
                .getArguments()[0]);
        assertThat(cfp.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(EXPECTED_CONTAINER_1, EXPECTED_CONTAINER_2));
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