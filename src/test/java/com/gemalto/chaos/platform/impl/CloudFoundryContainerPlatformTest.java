package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
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

import static com.gemalto.chaos.constants.CloudFoundryConstants.*;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryContainerPlatformTest {
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
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;
    String APPLICATION_ID = randomUUID().toString();

    @Before
    public void setUp () {
        cloudFoundryContainerPlatform = CloudFoundryContainerPlatform.builder()
                                                                     .withCloudFoundryClient(cloudFoundryClient)
                                                                     .withCloudFoundryOperations(cloudFoundryOperations)
                                                                     .withCloudFoundryPlatformInfo(cloudFoundryPlatformInfo)
                                                                     .withCloudFoundrySelfAwareness(cloudFoundrySelfAwareness)
                                                                     .withContainerManager(containerManager)
                                                                     .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getRoster () {
        String APPLICATION_NAME = randomUUID().toString();
        Integer INSTANCES = 2;
        CloudFoundryContainer EXPECTED_CONTAINER_1 = CloudFoundryContainer.builder()
                                                                          .applicationId(APPLICATION_ID)
                                                                          .instance(0)
                                                                          .platform(cloudFoundryContainerPlatform)
                                                                          .name(APPLICATION_NAME)
                                                                          .build();
        CloudFoundryContainer EXPECTED_CONTAINER_2 = CloudFoundryContainer.builder()
                                                                          .applicationId(APPLICATION_ID)
                                                                          .instance(1)
                                                                          .platform(cloudFoundryContainerPlatform)
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
        assertThat(cloudFoundryContainerPlatform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(EXPECTED_CONTAINER_1, EXPECTED_CONTAINER_2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void restartInstanceTest () {
        Mono<Void> monoVoid = mock(Mono.class);
        when(cloudFoundryOperations.applications()).thenReturn(applications);
        when(applications.restartInstance(null)).thenAnswer((Answer<Mono<Void>>) invocationOnMock -> monoVoid);
        cloudFoundryContainerPlatform.restartInstance(null);
        verify(applications, times(1)).restartInstance(null);
        verify(monoVoid, times(1)).block();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void restageApplicationTest () {
        Mono<Void> monoVoid = mock(Mono.class);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(monoVoid).when(applications).restage(null);
        cloudFoundryContainerPlatform.restageApplication(null);
        verify(applications, times(1)).restage(null);
        verify(monoVoid, times(1)).block();
    }

    @Test
    public void checkHealthDoesNotExist () {
        Integer INSTANCE_ID = 0;
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        ApplicationInstancesResponse applicationInstancesResponse = ApplicationInstancesResponse.builder().build();
        Mono<ApplicationInstancesResponse> applicationInstancesResponseMono = Mono.just(applicationInstancesResponse);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        doReturn(applicationInstancesResponseMono).when(applicationsV2)
                                                  .instances(any(ApplicationInstancesRequest.class));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, INSTANCE_ID));
    }

    @Test
    public void checkHealthUnderAttack () {
        Integer INSTANCE_ID = 0;
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        ApplicationInstancesResponse applicationInstancesResponse = ApplicationInstancesResponse.builder()
                                                                                                .instance(INSTANCE_ID.toString(), ApplicationInstanceInfo
                                                                                                        .builder()
                                                                                                        .consoleIp("")
                                                                                                        .consolePort(22)
                                                                                                        .debugIp("")
                                                                                                        .debugPort(8888)
                                                                                                        .details("")
                                                                                                        .since(0D)
                                                                                                        .state(CLOUDFOUNDRY_STARTING_STATE)
                                                                                                        .uptime(0L)
                                                                                                        .build())
                                                                                                .build();
        Mono<ApplicationInstancesResponse> applicationInstancesResponseMono = Mono.just(applicationInstancesResponse);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        doReturn(applicationInstancesResponseMono).when(applicationsV2)
                                                  .instances(any(ApplicationInstancesRequest.class));
        assertEquals(ContainerHealth.UNDER_ATTACK, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, INSTANCE_ID));
    }

    @Test
    public void checkHealthNormal () {
        Integer INSTANCE_ID = 0;
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        ApplicationInstancesResponse applicationInstancesResponse = ApplicationInstancesResponse.builder()
                                                                                                .instance(INSTANCE_ID.toString(), ApplicationInstanceInfo
                                                                                                        .builder()
                                                                                                        .consoleIp("")
                                                                                                        .consolePort(22)
                                                                                                        .debugIp("")
                                                                                                        .debugPort(8888)
                                                                                                        .details("")
                                                                                                        .since(0D)
                                                                                                        .state(CLOUDFOUNDRY_RUNNING_STATE)
                                                                                                        .uptime(0L)
                                                                                                        .build())
                                                                                                .build();
        Mono<ApplicationInstancesResponse> applicationInstancesResponseMono = Mono.just(applicationInstancesResponse);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        doReturn(applicationInstancesResponseMono).when(applicationsV2)
                                                  .instances(any(ApplicationInstancesRequest.class));
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, INSTANCE_ID));
    }
}