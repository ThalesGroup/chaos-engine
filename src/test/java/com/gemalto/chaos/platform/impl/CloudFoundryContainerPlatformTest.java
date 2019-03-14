package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.shellclient.ssh.SSHCredentials;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.advanced.Advanced;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

import static com.gemalto.chaos.constants.CloudFoundryConstants.*;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CloudFoundryContainerPlatformTest {
    @MockBean
    private CloudFoundryOperations cloudFoundryOperations;
    @SpyBean
    private ContainerManager containerManager;
    @Mock
    private Applications applications;
    @MockBean
    private CloudFoundryClient cloudFoundryClient;
    @MockBean
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;
    @Autowired
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;
    private String APPLICATION_ID = randomUUID().toString();
    private String APPLICATION_NAME = randomUUID().toString();
    private Integer INSTANCES = 2;
    private CloudFoundryContainer EXPECTED_CONTAINER_1 = CloudFoundryContainer.builder()
                                                                              .applicationId(APPLICATION_ID)
                                                                              .instance(0)
                                                                              .platform(cloudFoundryContainerPlatform)
                                                                              .name(APPLICATION_NAME)
                                                                              .build();

    @Test
    public void getRoster () {
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
                                                               .addAllUrls(Collections.emptySet())
                                                               .runningInstances(INSTANCES)
                                                               .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                                               .memoryLimit(0);
        ApplicationSummary applicationSummary = builder.build();
        ApplicationSummary stoppedApplicationSummary = builder.requestedState(CLOUDFOUNDRY_APPLICATION_STOPPED).build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary, stoppedApplicationSummary);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertThat(cloudFoundryContainerPlatform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(EXPECTED_CONTAINER_1, EXPECTED_CONTAINER_2));
    }

    @Test
    public void createContainersFromApplicationSummary () {
        ApplicationSummary applicationSummary;
        String name = UUID.randomUUID().toString();
        String applicationId = UUID.randomUUID().toString();
        Integer instances = new Random().nextInt(5) + 5;
        applicationSummary = ApplicationSummary.builder()
                                               .instances(instances)
                                               .name(name)
                                               .id(applicationId)
                                               .diskQuota(0)
                                               .memoryLimit(0)
                                               .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                               .runningInstances(instances)
                                               .build();
        CloudFoundryContainer[] containerCollection = IntStream.range(0, instances)
                                                               .mapToObj(instance -> CloudFoundryContainer.builder()
                                                                                                          .name(name)
                                                                                                          .applicationId(applicationId)
                                                                                                          .instance(instance)
                                                                                                          .build())
                                                               .toArray(CloudFoundryContainer[]::new);
        assertThat(cloudFoundryContainerPlatform.createContainersFromApplicationSummary(applicationSummary), IsIterableContainingInAnyOrder
                .containsInAnyOrder(containerCollection));
    }

    @Test
    public void createSingleContainerFromApplicationSummary () {
        ApplicationSummary applicationSummary;
        String name = UUID.randomUUID().toString();
        String applicationId = UUID.randomUUID().toString();
        Integer instances = new Random().nextInt(5) + 5;
        Integer index = new Random().nextInt(instances);
        applicationSummary = ApplicationSummary.builder()
                                               .instances(instances)
                                               .name(name)
                                               .id(applicationId)
                                               .diskQuota(0)
                                               .memoryLimit(0)
                                               .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                               .runningInstances(instances)
                                               .build();
        CloudFoundryContainer expectedContainer = CloudFoundryContainer.builder()
                                                                       .applicationId(applicationId)
                                                                       .name(name)
                                                                       .instance(index)
                                                                       .build();
        CloudFoundryContainer actualContainer = cloudFoundryContainerPlatform.createSingleContainerFromApplicationSummary(applicationSummary, index);
        assertEquals(expectedContainer, actualContainer);
        verify(containerManager, times(1)).offer(actualContainer);
        reset(containerManager);
        assertSame(actualContainer, cloudFoundryContainerPlatform.createSingleContainerFromApplicationSummary(applicationSummary, index));
        verify(containerManager, times(0)).offer(actualContainer);
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
    public void checkHealthUnderExperiment () {
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
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, INSTANCE_ID));
    }

    @Test
    public void checkHealthIgnorableException () {
        doThrow(new ClientV2Exception(0, 170002, "App has not finished staging", "CF-NotStaged")).when(cloudFoundryClient)
                                                                                                 .applicationsV2();
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, 0));
    }

    @Test
    public void checkHealthException () {
        doThrow(new ClientV2Exception(0, 170001, "Any other exception", "CF-NotStaged")).when(cloudFoundryClient)
                                                                                        .applicationsV2();
        assertEquals(ContainerHealth.DOES_NOT_EXIST, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, 0));
        doThrow(new ClientV2Exception(0, 170002, "Any other exception", "CF-Staged")).when(cloudFoundryClient)
                                                                                     .applicationsV2();
        assertEquals(ContainerHealth.DOES_NOT_EXIST, cloudFoundryContainerPlatform.checkHealth(APPLICATION_ID, 0));
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

    @Test
    public void getEndpoint () {
        Info info = mock(Info.class);
        String expectedEndpoint = UUID.randomUUID().toString();
        GetInfoResponse infoResponse = GetInfoResponse.builder().applicationSshEndpoint(expectedEndpoint).build();
        doReturn(Mono.just(infoResponse)).when(info).get(any());
        doReturn(info).when(cloudFoundryClient).info();
        assertEquals(expectedEndpoint, cloudFoundryContainerPlatform.getEndpoint(null));
    }

    @Test(expected = ChaosException.class)
    public void getEndpointException () {
        Info info = mock(Info.class);
        String expectedEndpoint = UUID.randomUUID().toString();
        doReturn(Mono.empty()).when(info).get(any());
        doReturn(info).when(cloudFoundryClient).info();
        assertEquals(expectedEndpoint, cloudFoundryContainerPlatform.getEndpoint(null));
    }

    @Test
    public void getSshCredentials () throws Exception {
        CloudFoundryContainer container;
        Integer index = new Random().nextInt(100) + 1;
        String applicationId = UUID.randomUUID().toString();
        String fakePassword = UUID.randomUUID().toString();
        container = CloudFoundryContainer.builder().applicationId(applicationId).instance(index).build();
        SSHCredentials credentials = cloudFoundryContainerPlatform.getSshCredentials(container);
        assertEquals("cf:" + applicationId + "/" + index.toString(), credentials.getUsername());
        doReturn(fakePassword).when(cloudFoundryContainerPlatform).getSSHOneTimePassword();
        assertEquals(fakePassword, credentials.getPasswordGenerator().call());
        assertThat(credentials.getSSHKeys().size(), is(0));
    }

    @Test
    public void recycleContainer () {
        Integer index = new Random().nextInt(100) + 1;
        String name = UUID.randomUUID().toString();
        CloudFoundryContainer container = CloudFoundryContainer.builder().name(name).instance(index).build();
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(Mono.empty()).when(applications).restartInstance(any());
        cloudFoundryContainerPlatform.recycleContainer(container);
        verify(applications, times(1)).restartInstance(RestartApplicationInstanceRequest.builder()
                                                                                        .name(name)
                                                                                        .instanceIndex(index)
                                                                                        .build());
    }

    @Test
    public void getOneTimePassword () {
        String sshCode = randomUUID().toString();
        Advanced advanced = mock(Advanced.class);
        doReturn(advanced).when(cloudFoundryOperations).advanced();
        doReturn(Mono.just(sshCode)).when(advanced).sshCode();
        assertEquals(sshCode, cloudFoundryContainerPlatform.getSSHOneTimePassword());
    }

    @Test(expected = ChaosException.class)
    public void getOneTimePasswordException () {
        Advanced advanced = mock(Advanced.class);
        doReturn(advanced).when(cloudFoundryOperations).advanced();
        doReturn(Mono.empty()).when(advanced).sshCode();
        cloudFoundryContainerPlatform.getSSHOneTimePassword();
    }

    @Test
    public void getTimeInState () {
        String applicationId = UUID.randomUUID().toString();
        Integer index = new Random().nextInt(100) + 1;
        CloudFoundryContainer container = CloudFoundryContainer.builder()
                                                               .applicationId(applicationId)
                                                               .instance(index)
                                                               .build();
        // Return an applicationsV2 mock
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        // Calculate a time that it is up since
        Double since = 1552569958455D; // Pi day!
        // Add the uptime into an ApplicationInstanceInfo
        ApplicationInstanceInfo instanceInfo = ApplicationInstanceInfo.builder().since(since).build();
        // Return a mono of an ApplicationInstancesResponse
        ApplicationInstancesResponse response = ApplicationInstancesResponse.builder()
                                                                            .instance(index.toString(), instanceInfo)
                                                                            .build();
        doReturn(Mono.just(response)).when(applicationsV2)
                                     .instances(ApplicationInstancesRequest.builder()
                                                                           .applicationId(applicationId)
                                                                           .build());
        Instant expected = DateTimeFormatter.ISO_INSTANT.parse("2019-03-14T13:25:58.455Z", Instant::from);
        assertEquals(expected, cloudFoundryContainerPlatform.getTimeInState(container));
    }

    @Test(expected = ChaosException.class)
    public void getTimeInStateChaosException () {
        String applicationId = UUID.randomUUID().toString();
        Integer index = new Random().nextInt(100) + 1;
        CloudFoundryContainer container = CloudFoundryContainer.builder()
                                                               .applicationId(applicationId)
                                                               .instance(index)
                                                               .build();
        // Return an applicationsV2 mock
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        // Return a mono of an ApplicationInstancesResponse
        doReturn(Mono.empty()).when(applicationsV2)
                              .instances(ApplicationInstancesRequest.builder().applicationId(applicationId).build());
        cloudFoundryContainerPlatform.getTimeInState(container);
    }

    @Test
    public void isContainerRecycled () {
        CloudFoundryContainer container = mock(CloudFoundryContainer.class);
        String applicationId = UUID.randomUUID().toString();
        Integer index = new Random().nextInt(100) + 1;
        doReturn(applicationId).when(container).getApplicationId();
        doReturn(index).when(container).getInstance();
        Map<ContainerHealth, Boolean> containerHealthMap;
        containerHealthMap = new HashMap<>();
        containerHealthMap.put(ContainerHealth.NORMAL, Boolean.TRUE);
        containerHealthMap.put(ContainerHealth.RUNNING_EXPERIMENT, Boolean.FALSE);
        containerHealthMap.put(ContainerHealth.DOES_NOT_EXIST, Boolean.FALSE);
        for (Map.Entry<ContainerHealth, Boolean> entry : containerHealthMap.entrySet()) {
            doReturn(entry.getKey()).when(cloudFoundryContainerPlatform).checkHealth(applicationId, index);
            doReturn(Instant.ofEpochMilli(1552569958455L)).when(cloudFoundryContainerPlatform)
                                                          .getTimeInState(container);
            doReturn(Instant.ofEpochMilli(1552569958454L), Instant.ofEpochMilli(1552569958456L)).when(container)
                                                                                                .getExperimentStartTime();
            assertEquals(entry.getValue(), cloudFoundryContainerPlatform.isContainerRecycled(container));
            assertFalse(cloudFoundryContainerPlatform.isContainerRecycled(container));
        }
    }

    @Test
    public void isContainerRecycledError () {
        Container container = mock(Container.class);
        assertFalse(cloudFoundryContainerPlatform.isContainerRecycled(container));
    }

    @Configuration
    static class ContextConfiguration {

        @Bean
        CloudFoundryContainerPlatform cloudFoundryContainerPlatform () {
            return spy(new CloudFoundryContainerPlatform());
        }
    }
}