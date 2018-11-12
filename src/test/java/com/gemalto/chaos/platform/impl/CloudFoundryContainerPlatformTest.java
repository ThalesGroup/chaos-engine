package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.SshManager;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.impl.experiments.RandomProcessTermination;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.gemalto.chaos.constants.CloudFoundryConstants.*;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CloudFoundryContainerPlatformTest {
    @MockBean
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
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
    @Mock
    ShResourceService shResourceService;
    private String APPLICATION_ID = randomUUID().toString();

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
    public void sshExperimentMergeCapabilities () throws IOException {
        ArrayList<ShellSessionCapability> alreadyDetectedCapabilities = new ArrayList<>();
        alreadyDetectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.ASH));
        ArrayList<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.ASH));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.TYPE));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.GREP));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.KILL));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.SORT));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.HEAD));
        SshManager sshManager = mock(SshManager.class);
        RandomProcessTermination term = new RandomProcessTermination();
        term.setShellSessionCapabilities(alreadyDetectedCapabilities);
        SshCommandResult resultShellCapability = mock(SshCommandResult.class);
        SshCommandResult resultTypeCapability = mock(SshCommandResult.class);
        SshCommandResult resultGrepCapability = mock(SshCommandResult.class);
        SshCommandResult resultKillCapability = mock(SshCommandResult.class);
        SshCommandResult resultSortCapability = mock(SshCommandResult.class);
        SshCommandResult resultHeadCapability = mock(SshCommandResult.class);
        when(resultShellCapability.getExitStatus()).thenReturn(0);
        when(resultShellCapability.getCommandOutput()).thenReturn("bash");
        when(resultTypeCapability.getExitStatus()).thenReturn(0);
        when(resultTypeCapability.getCommandOutput()).thenReturn("type");
        when(resultGrepCapability.getExitStatus()).thenReturn(0);
        when(resultGrepCapability.getCommandOutput()).thenReturn("grep");
        when(resultKillCapability.getExitStatus()).thenReturn(0);
        when(resultKillCapability.getCommandOutput()).thenReturn("kill");
        when(resultSortCapability.getExitStatus()).thenReturn(0);
        when(resultSortCapability.getCommandOutput()).thenReturn("sort");
        when(resultHeadCapability.getExitStatus()).thenReturn(0);
        when(resultHeadCapability.getCommandOutput()).thenReturn("head");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(resultShellCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.GREP)).thenReturn(resultGrepCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.KILL)).thenReturn(resultKillCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.SORT)).thenReturn(resultSortCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.HEAD)).thenReturn(resultHeadCapability);
        term.setSshManager(sshManager);
        term.setShResourceService(shResourceService);
        term.runExperiment();
        assertEquals(expectedCapabilities.size(), term.getShellSessionCapabilities().size());
        Assert.assertEquals(expectedCapabilities, term.getShellSessionCapabilities());
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private CloudFoundryOperations cloudFoundryOperations;
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
        @Autowired
        private CloudFoundryClient cloudFoundryClient;
        @Autowired
        private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

        @Bean
        CloudFoundryContainerPlatform cloudFoundryContainerPlatform () {
            return spy(CloudFoundryContainerPlatform.builder()
                                                    .withCloudFoundryClient(cloudFoundryClient)
                                                    .withCloudFoundryOperations(cloudFoundryOperations)
                                                    .withCloudFoundryPlatformInfo(cloudFoundryPlatformInfo)
                                                    .withCloudFoundrySelfAwareness(cloudFoundrySelfAwareness)
                                                    .withContainerManager(containerManager)
                                                    .build());
        }
    }
}