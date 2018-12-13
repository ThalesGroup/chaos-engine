package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.SshCommandResult;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.impl.CloudFoundrySshManager;
import com.gemalto.chaos.ssh.impl.experiments.RandomProcessTermination;
import com.gemalto.chaos.ssh.services.ShResourceService;
import com.gemalto.chaos.util.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import java.util.*;
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
    @MockBean
    private ShResourceService shResourceService;
    private String APPLICATION_ID = randomUUID().toString();
    private String APPLICATION_NAME = randomUUID().toString();
    private Integer INSTANCES = 2;
    private CloudFoundryContainer EXPECTED_CONTAINER_1 = CloudFoundryContainer.builder()
                                                                              .applicationId(APPLICATION_ID)
                                                                              .instance(0)
                                                                              .platform(cloudFoundryContainerPlatform)
                                                                              .name(APPLICATION_NAME)
                                                                              .build();
    private String command = StringUtils.generateRandomString(100);


    @Test
    @SuppressWarnings("unchecked")
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
    public void sshExperiment () throws IOException {
        CloudFoundryContainer container = Mockito.spy(CloudFoundryContainer.builder()
                                                                           .applicationId(APPLICATION_ID)
                                                                           .instance(0)
                                                                           .platform(cloudFoundryContainerPlatform)
                                                                           .build());
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        when(sshManager.connect(container)).thenReturn(true);
        SshCommandResult result = mock(SshCommandResult.class);
        when(result.getExitStatus()).thenReturn(0);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
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
        RandomProcessTermination term = Mockito.spy(new RandomProcessTermination());
        cloudFoundryContainerPlatform.sshExperiment(term, container);
        verify(term, times(0)).setDetectedShellSessionCapabilities(ArgumentMatchers.anyList());
        verify(container, times(1)).setDetectedCapabilities(ArgumentMatchers.anyList());
        verify(term, times(1)).runExperiment();
        verify(sshManager, times(1)).disconnect();
    }

    @Test
    public void sshExperimentMergePreviouslyDetectedCapabilities () throws IOException {
        CloudFoundryContainer container = Mockito.spy(CloudFoundryContainer.builder()
                                                                           .applicationId(APPLICATION_ID)
                                                                           .instance(0)
                                                                           .platform(cloudFoundryContainerPlatform)
                                                                           .build());

        List<ShellSessionCapability> alreadyDetectedCapabilities = new ArrayList<>();
        alreadyDetectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.ASH));
        container.setDetectedCapabilities(alreadyDetectedCapabilities);
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        when(sshManager.connect(container)).thenReturn(true);
        SshCommandResult result = mock(SshCommandResult.class);
        when(result.getExitStatus()).thenReturn(0);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();

        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.ASH));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.TYPE));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.GREP));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.KILL));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.SORT));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.HEAD));

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
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.GREP)).thenReturn(resultGrepCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.KILL)).thenReturn(resultKillCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.SORT)).thenReturn(resultSortCapability);
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.HEAD)).thenReturn(resultHeadCapability);
        RandomProcessTermination term = Mockito.spy(new RandomProcessTermination());
        cloudFoundryContainerPlatform.sshExperiment(term, container);
        verify(term, times(1)).setDetectedShellSessionCapabilities(ArgumentMatchers.anyList());
        verify(container, times(1)).setDetectedCapabilities(ArgumentMatchers.anyList());
        verify(term, times(1)).runExperiment();
        verify(sshManager, times(1)).disconnect();
        assertThat(expectedCapabilities, IsIterableContainingInAnyOrder.containsInAnyOrder(container.getDetectedCapabilities()
                                                                                                    .toArray()));
    }

    @Test(expected = ChaosException.class)
    public void sshExperimentFailedToConnect () throws IOException {
        CloudFoundryContainer container = Mockito.spy(CloudFoundryContainer.builder()
                                                                           .applicationId(APPLICATION_ID)
                                                                           .instance(0)
                                                                           .platform(cloudFoundryContainerPlatform)
                                                                           .build());
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        when(sshManager.connect(container)).thenThrow(IOException.class);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
        RandomProcessTermination term = Mockito.spy(new RandomProcessTermination());
        cloudFoundryContainerPlatform.sshExperiment(term, container);
        verify(sshManager, times(1)).disconnect();
    }

    @Test
    public void sshBasedHealthCheck () throws IOException {

        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        SshCommandResult result = mock(SshCommandResult.class);
        when(result.getExitStatus()).thenReturn(0);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
        doReturn(result).when(sshManager).executeCommand(command);
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainerPlatform.sshBasedHealthCheck(EXPECTED_CONTAINER_1, command, 0));
        verify(sshManager, times(1)).disconnect();
        when(result.getExitStatus()).thenReturn(-1);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.sshBasedHealthCheck(EXPECTED_CONTAINER_1, command, 0));
        when(result.getExitStatus()).thenReturn(127);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.sshBasedHealthCheck(EXPECTED_CONTAINER_1, command, 0));
    }

    @Test
    public void sshBasedHealthCheckFailed () throws IOException {
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
        doThrow(IOException.class).when(sshManager).connect(ArgumentMatchers.any());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.sshBasedHealthCheck(EXPECTED_CONTAINER_1, command, 0));
    }

    @Test
    public void sshBasedHealthCheckInverse () throws IOException {
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        SshCommandResult result = mock(SshCommandResult.class);
        when(result.getExitStatus()).thenReturn(0);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
        doReturn(result).when(sshManager).executeCommand(command);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.sshBasedHealthCheckInverse(EXPECTED_CONTAINER_1, command, 0));
        verify(sshManager, times(1)).disconnect();
        when(result.getExitStatus()).thenReturn(-1);
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainerPlatform.sshBasedHealthCheckInverse(EXPECTED_CONTAINER_1, command, 0));
        when(result.getExitStatus()).thenReturn(127);
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainerPlatform.sshBasedHealthCheckInverse(EXPECTED_CONTAINER_1, command, 0));
    }

    @Test
    public void sshBasedHealthCheckInverseFailed () throws IOException {
        CloudFoundrySshManager sshManager = mock(CloudFoundrySshManager.class);
        doReturn(sshManager).when(cloudFoundryContainerPlatform).getSSHManager();
        doThrow(IOException.class).when(sshManager).connect(ArgumentMatchers.any());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, cloudFoundryContainerPlatform.sshBasedHealthCheckInverse(EXPECTED_CONTAINER_1, command, 0));
    }

    @Configuration
    static class ContextConfiguration {

        @Bean
        CloudFoundryContainerPlatform cloudFoundryContainerPlatform () {
            return spy(new CloudFoundryContainerPlatform());
        }
    }
}