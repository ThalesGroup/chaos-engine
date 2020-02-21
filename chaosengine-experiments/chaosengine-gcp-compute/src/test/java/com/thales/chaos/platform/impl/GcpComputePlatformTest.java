/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.platform.impl;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.compute.v1.*;
import com.thales.chaos.constants.GcpConstants;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpComputeInstanceContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.selfawareness.GcpComputeSelfAwareness;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.thales.chaos.services.impl.GcpComputeService.COMPUTE_PROJECT;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GcpComputePlatformTest {
    private static final String MY_AWESOME_PROJECT = "my-awesome-project";
    @MockBean
    private InstanceClient instanceClient;
    @MockBean
    private InstanceGroupClient instanceGroupClient;
    @MockBean
    private InstanceGroupManagerClient instanceGroupManagerClient;
    @MockBean
    private RegionInstanceGroupClient regionInstanceGroupClient;
    @MockBean
    private RegionInstanceGroupManagerClient regionInstanceGroupManagerClient;
    @MockBean
    private ZoneOperationClient zoneOperationClient;
    @MockBean
    private GcpComputeSelfAwareness selfAwareness;
    @Autowired
    private ProjectName projectName;
    @Autowired
    private GcpComputePlatform gcpComputePlatform;

    @Test
    public void isIAASPlatform () {
        assertEquals(PlatformLevel.IAAS, gcpComputePlatform.getPlatformLevel());
    }

    @Test
    public void createContainerFromInstanceWithNoDetails () {
        Instance instance = Instance.newBuilder().build();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder().build();
        assertEquals(container, gcpComputePlatform.createContainerFromInstance(instance));
    }

    @Test
    public void createContainerFromInstanceWithDetails () {
        String createdByValue = "My-VM-Instance-Group";
        Metadata metadata = Metadata.newBuilder()
                                    .addItems(Items.newBuilder()
                                                   .setKey(GcpConstants.CREATED_BY_METADATA_KEY)
                                                   .setValue(createdByValue)
                                                   .build())
                                    .build();
        String tag1 = "HTTP";
        String tag2 = "SSH";
        String tag3 = "RDP";
        Tags tags = Tags.newBuilder().addAllItems(List.of(tag1, tag2, tag3)).build();
        String id = "123456789101112131415";
        String name = "My-Weird-SSH-and-RDP-Web-Server";
        String zone = "some-datacenter-somewhere";
        Instance instance = Instance.newBuilder()
                                    .setMetadata(metadata)
                                    .setName(name)
                                    .setId(id)
                                    .setTags(tags)
                                    .setZone(zone)
                                    .build();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(createdByValue)
                                                                           .withPlatform(gcpComputePlatform)
                                                                           .withInstanceName(name)
                                                                           .withUniqueIdentifier(id)
                                                                           .withZone(zone)
                                                                           .build();
        assertEquals(container, gcpComputePlatform.createContainerFromInstance(instance));
    }

    @Test
    public void generateRoster () {
        GcpComputeInstanceContainer expected = GcpComputeInstanceContainer.builder()
                                                                          .withUniqueIdentifier("12345678901234567890")
                                                                          .build();
        InstanceClient.AggregatedListInstancesPagedResponse response = mock(InstanceClient.AggregatedListInstancesPagedResponse.class);
        Instance instance = Instance.newBuilder().setId("12345678901234567890").build();
        Iterable<InstancesScopedList> iterableInstances = List.of(InstancesScopedList.newBuilder()
                                                                                     .addInstances(instance)
                                                                                     .build());
        doReturn(iterableInstances).when(response).iterateAll();
        doReturn(response).when(instanceClient).aggregatedListInstances(projectName);
        assertThat(gcpComputePlatform.generateRoster(), containsInAnyOrder(expected));
        verify(gcpComputePlatform).isNotFiltered(instance);
    }

    @Test
    public void generateRosterWithChaosEngineHost () {
        GcpComputeInstanceContainer expected = GcpComputeInstanceContainer.builder()
                                                                          .withUniqueIdentifier("12345678901234567890")
                                                                          .build();
        InstanceClient.AggregatedListInstancesPagedResponse response = mock(InstanceClient.AggregatedListInstancesPagedResponse.class);
        Instance instance = Instance.newBuilder().setId("12345678901234567890").build();
        Instance chaosEngineHost = Instance.newBuilder().setId("31415926535897").build();
        Iterable<InstancesScopedList> iterableInstances = List.of(InstancesScopedList.newBuilder()
                                                                                     .addInstances(instance)
                                                                                     .addInstances(chaosEngineHost)
                                                                                     .build());
        doReturn(iterableInstances).when(response).iterateAll();
        doReturn(response).when(instanceClient).aggregatedListInstances(projectName);
        doReturn(true).when(selfAwareness).isMe(31415926535897L);
        assertThat(gcpComputePlatform.generateRoster(), containsInAnyOrder(expected));
        verify(gcpComputePlatform).isNotFiltered(instance);
    }

    @Test
    public void startInstance () {
        String zone = "my-zone";
        String uniqueIdentifier = "12345678901234567890";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueIdentifier)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = GcpComputePlatform.getProjectZoneInstanceNameOfContainer(container,
                projectName);
        gcpComputePlatform.startInstance(container);
        verify(instanceClient).startInstance(instanceName);
    }

    @Test
    public void stopInstance () {
        String zone = "my-zone";
        String uniqueIdentifier = "12345678901234567890";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueIdentifier)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = GcpComputePlatform.getProjectZoneInstanceNameOfContainer(container,
                projectName);
        Operation operation = Operation.newBuilder().setSelfLink("my-operation").build();
        doReturn(operation).when(instanceClient).stopInstance(instanceName);
        assertEquals("my-operation", gcpComputePlatform.stopInstance(container));
    }


    @Test
    public void getProjectZoneInstanceName () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withZone("my-zone")
                                                                           .withUniqueIdentifier("unique-identifier")
                                                                           .build();
        ProjectZoneInstanceName actualInstanceName = GcpComputePlatform.getProjectZoneInstanceNameOfContainer(container,
                projectName);
        ProjectZoneInstanceName expectedInstanceName = ProjectZoneInstanceName.newBuilder()
                                                                              .setZone("my-zone")
                                                                              .setInstance("unique-identifier")
                                                                              .setProject(MY_AWESOME_PROJECT)
                                                                              .build();
        assertEquals(actualInstanceName, expectedInstanceName);
    }

    @Test
    public void isNotFiltered () {
        Items includeTags = Items.newBuilder().setKey("include").setValue("true").build();
        Metadata includeMetadata = Metadata.newBuilder().addItems(includeTags).build();
        Items excludeTags = Items.newBuilder().setKey("exclude").setValue("true").build();
        Metadata excludeMetadata = Metadata.newBuilder().addItems(excludeTags).build();
        Instance matchingInclude = Instance.newBuilder().setMetadata(includeMetadata).build();
        Instance includeWithExclude = Instance.newBuilder(matchingInclude).setMetadata(excludeMetadata).build();
        gcpComputePlatform.setIncludeFilter(Map.of("include", "true"));
        gcpComputePlatform.setExcludeFilter(Map.of("exclude", "true"));
        assertTrue(gcpComputePlatform.isNotFiltered(matchingInclude));
        assertFalse(gcpComputePlatform.isNotFiltered(includeWithExclude));
    }

    @Test
    public void isContainerGroupAtDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectZoneInstanceGroupManagerName.of(
                                                                                           INSTANCE_GROUP,
                                                                                           PROJECT,
                                                                                           ZONE)
                                                                                                                      .toString()
                                                                                                                      .substring(
                                                                                                                              ProjectZoneInstanceGroupManagerName.SERVICE_ADDRESS
                                                                                                                                      .length() - "projects/"
                                                                                                                                      .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(10).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(instanceGroupClient)
                               .getInstanceGroup(ProjectZoneInstanceGroupName.of(INSTANCE_GROUP, PROJECT, ZONE));
        doReturn(instanceGroupManager).when(instanceGroupManagerClient)
                                      .getInstanceGroupManager(ProjectZoneInstanceGroupManagerName.of(INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerGroupBelowDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(ProjectZoneInstanceGroupName.of(
                                                                                   INSTANCE_GROUP,
                                                                                   PROJECT,
                                                                                   ZONE)
                                                                                                                      .toString()
                                                                                                                      .substring(
                                                                                                                              ProjectRegionInstanceGroupName.SERVICE_ADDRESS
                                                                                                                                      .length() - "projects/"
                                                                                                                                      .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(9).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(instanceGroupClient)
                               .getInstanceGroup(ProjectZoneInstanceGroupName.of(INSTANCE_GROUP, PROJECT, ZONE));
        doReturn(instanceGroupManager).when(instanceGroupManagerClient)
                                      .getInstanceGroupManager(ProjectZoneInstanceGroupManagerName.of(INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertFalse(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerGroupAboveDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectZoneInstanceGroupManagerName.of(
                                                                                           INSTANCE_GROUP,
                                                                                           PROJECT,
                                                                                           ZONE)
                                                                                                                      .toString()
                                                                                                                      .substring(
                                                                                                                              ProjectZoneInstanceGroupManagerName.SERVICE_ADDRESS
                                                                                                                                      .length() - "projects/"
                                                                                                                                      .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(11).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(instanceGroupClient)
                               .getInstanceGroup(ProjectZoneInstanceGroupName.of(INSTANCE_GROUP, PROJECT, ZONE));
        doReturn(instanceGroupManager).when(instanceGroupManagerClient)
                                      .getInstanceGroupManager(ProjectZoneInstanceGroupManagerName.of(INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerRegionGroupAtDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectRegionInstanceGroupManagerName
                                                                                           .of(INSTANCE_GROUP,
                                                                                                   PROJECT,
                                                                                                   ZONE)
                                                                                           .toString()
                                                                                           .substring(
                                                                                                   ProjectRegionInstanceGroupManagerName.SERVICE_ADDRESS
                                                                                                           .length() - "projects/"
                                                                                                           .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(10).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(regionInstanceGroupClient)
                               .getRegionInstanceGroup(ProjectRegionInstanceGroupName.of(INSTANCE_GROUP,
                                       PROJECT,
                                       ZONE));
        doReturn(instanceGroupManager).when(regionInstanceGroupManagerClient)
                                      .getRegionInstanceGroupManager(ProjectRegionInstanceGroupManagerName.of(
                                              INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerRegionGroupBelowDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(ProjectRegionInstanceGroupName
                                                                                   .of(INSTANCE_GROUP, PROJECT, ZONE)
                                                                                   .toString()
                                                                                   .substring(
                                                                                           ProjectRegionInstanceGroupName.SERVICE_ADDRESS
                                                                                                   .length() - "projects/"
                                                                                                   .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(10).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(11).build();
        doReturn(instanceGroup).when(regionInstanceGroupClient)
                               .getRegionInstanceGroup(ProjectRegionInstanceGroupName.of(INSTANCE_GROUP,
                                       PROJECT,
                                       ZONE));
        doReturn(instanceGroupManager).when(regionInstanceGroupManagerClient)
                                      .getRegionInstanceGroupManager(ProjectRegionInstanceGroupManagerName.of(
                                              INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertFalse(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerRegionGroupAboveDesiredCapacity () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectRegionInstanceGroupManagerName
                                                                                           .of(INSTANCE_GROUP,
                                                                                                   PROJECT,
                                                                                                   ZONE)
                                                                                           .toString()
                                                                                           .substring(
                                                                                                   ProjectRegionInstanceGroupName.SERVICE_ADDRESS
                                                                                                           .length() - "projects/"
                                                                                                           .length()))
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(11).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(regionInstanceGroupClient)
                               .getRegionInstanceGroup(ProjectRegionInstanceGroupName.of(INSTANCE_GROUP,
                                       PROJECT,
                                       ZONE));
        doReturn(instanceGroupManager).when(regionInstanceGroupManagerClient)
                                      .getRegionInstanceGroupManager(ProjectRegionInstanceGroupManagerName.of(
                                              INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerGroupAtDesiredCapacityWithFullProjectLink () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectZoneInstanceGroupManagerName.of(
                                                                                           INSTANCE_GROUP,
                                                                                           PROJECT,
                                                                                           ZONE).toString())
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(10).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(instanceGroupClient)
                               .getInstanceGroup(ProjectZoneInstanceGroupName.of(INSTANCE_GROUP, PROJECT, ZONE));
        doReturn(instanceGroupManager).when(instanceGroupManagerClient)
                                      .getInstanceGroupManager(ProjectZoneInstanceGroupManagerName.of(INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void isContainerRegionGroupAtDesiredCapacityWithFullProjectLink () {
        final String INSTANCE_GROUP = "12345";
        final String PROJECT = "54321";
        final String ZONE = "my-datacenter";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(
                                                                                   ProjectRegionInstanceGroupManagerName
                                                                                           .of(INSTANCE_GROUP,
                                                                                                   PROJECT,
                                                                                                   ZONE)
                                                                                           .toString())
                                                                           .build();
        InstanceGroup instanceGroup = InstanceGroup.newBuilder().setSize(10).build();
        InstanceGroupManager instanceGroupManager = InstanceGroupManager.newBuilder().setTargetSize(10).build();
        doReturn(instanceGroup).when(regionInstanceGroupClient)
                               .getRegionInstanceGroup(ProjectRegionInstanceGroupName.of(INSTANCE_GROUP,
                                       PROJECT,
                                       ZONE));
        doReturn(instanceGroupManager).when(regionInstanceGroupManagerClient)
                                      .getRegionInstanceGroupManager(ProjectRegionInstanceGroupManagerName.of(
                                              INSTANCE_GROUP,
                                              PROJECT,
                                              ZONE));
        assertTrue(gcpComputePlatform.isContainerGroupAtCapacity(container));
    }

    @Test
    public void restartContainer () {
        String zone = "my-zone";
        String uniqueIdentifier = "12345678901234567890";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueIdentifier)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = GcpComputePlatform.getProjectZoneInstanceNameOfContainer(container,
                projectName);
        Operation operation = Operation.newBuilder().setSelfLink("my-operation").build();
        doReturn(operation).when(instanceClient).resetInstance(instanceName);
        assertEquals("my-operation", gcpComputePlatform.restartContainer(container));
    }

    @Test
    public void isOperationCompletePartialName () {
        String project = MY_AWESOME_PROJECT;
        String zone = "my-datacenter";
        String operationName = "my-operation";
        String operationId = String.format("%s/zones/%s/operations/%s", project, zone, operationName);
        ProjectZoneOperationName projectZoneOperation = ProjectZoneOperationName.of(operationName, project, zone);
        Operation operation = mock(Operation.class);
        doReturn(operation).when(zoneOperationClient).getZoneOperation(projectZoneOperation);
        doReturn(99, 100, 101).when(operation).getProgress();
        assertFalse(gcpComputePlatform.isOperationComplete(operationId));
        assertTrue(gcpComputePlatform.isOperationComplete(operationId));
        assertTrue(gcpComputePlatform.isOperationComplete(operationId));
    }

    @Test
    public void isOperationCompleteFullName () {
        String project = MY_AWESOME_PROJECT;
        String zone = "my-datacenter";
        String operationName = "my-operation";
        String operationId = String.format(ProjectZoneOperationName.SERVICE_ADDRESS + "%s/zones/%s/operations/%s",
                project,
                zone,
                operationName);
        ProjectZoneOperationName projectZoneOperation = ProjectZoneOperationName.of(operationName, project, zone);
        Operation operation = mock(Operation.class);
        doReturn(operation).when(zoneOperationClient).getZoneOperation(projectZoneOperation);
        doReturn(99, 100, 101).when(operation).getProgress();
        assertFalse(gcpComputePlatform.isOperationComplete(operationId));
        assertTrue(gcpComputePlatform.isOperationComplete(operationId));
        assertTrue(gcpComputePlatform.isOperationComplete(operationId));
    }

    @Test
    public void isContainerRunningDeleted () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instance = ProjectZoneInstanceName.newBuilder()
                                                                  .setProject(MY_AWESOME_PROJECT)
                                                                  .setZone(zone)
                                                                  .setInstance(uniqueId)
                                                                  .build();
        doThrow(new ApiException(new RuntimeException(), new StatusCode() {
            @Override
            public Code getCode () {
                return Code.NOT_FOUND;
            }

            @Override
            public Object getTransportCode () {
                return null;
            }
        }, false)).when(instanceClient).getInstance(instance);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, gcpComputePlatform.isContainerRunning(container));
    }

    @Test
    public void isContainerRunningGenericError () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instance = ProjectZoneInstanceName.newBuilder()
                                                                  .setProject(MY_AWESOME_PROJECT)
                                                                  .setZone(zone)
                                                                  .setInstance(uniqueId)
                                                                  .build();
        doThrow(new ApiException(new RuntimeException(), new StatusCode() {
            @Override
            public Code getCode () {
                return Code.UNKNOWN;
            }

            @Override
            public Object getTransportCode () {
                return null;
            }
        }, false)).when(instanceClient).getInstance(instance);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, gcpComputePlatform.isContainerRunning(container));
    }

    @Test
    public void isContainerRunningTerminated () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.newBuilder()
                                                                      .setProject(MY_AWESOME_PROJECT)
                                                                      .setZone(zone)
                                                                      .setInstance(uniqueId)
                                                                      .build();
        Instance instance = mock(Instance.class);
        doReturn("TERMINATED").when(instance).getStatus();
        doReturn(instance).when(instanceClient).getInstance(instanceName);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, gcpComputePlatform.isContainerRunning(container));
    }

    @Test
    public void isContainerRunningStopped () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.newBuilder()
                                                                      .setProject(MY_AWESOME_PROJECT)
                                                                      .setZone(zone)
                                                                      .setInstance(uniqueId)
                                                                      .build();
        Instance instance = mock(Instance.class);
        doReturn("STOPPED").when(instance).getStatus();
        doReturn(instance).when(instanceClient).getInstance(instanceName);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, gcpComputePlatform.isContainerRunning(container));
    }

    @Test
    public void isContainerRunningYesItIs () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.newBuilder()
                                                                      .setProject(MY_AWESOME_PROJECT)
                                                                      .setZone(zone)
                                                                      .setInstance(uniqueId)
                                                                      .build();
        Instance instance = mock(Instance.class);
        doReturn("RUNNING").when(instance).getStatus();
        doReturn(instance).when(instanceClient).getInstance(instanceName);
        assertEquals(ContainerHealth.NORMAL, gcpComputePlatform.isContainerRunning(container));
    }

    @Test
    public void simulateMaintenance () {
        String zone = "my-zone";
        String uniqueId = "my-unique-id";
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier(uniqueId)
                                                                           .withZone(zone)
                                                                           .build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.newBuilder()
                                                                      .setProject(MY_AWESOME_PROJECT)
                                                                      .setZone(zone)
                                                                      .setInstance(uniqueId)
                                                                      .build();
        Operation operation = mock(Operation.class);
        doReturn(operation).when(instanceClient).simulateMaintenanceEventInstance(instanceName);
        doReturn("self-link").when(operation).getSelfLink();
        assertEquals("self-link", gcpComputePlatform.simulateMaintenance(container));
    }

    @Test
    public void recreateInstanceInRegionalInstanceGroup () {
        String uuid = "12345678901234567890";
        String instanceName = "my-awesome-instance";
        String zone = "my-datacenter";
        String region = "my-city";
        String instanceGroup = "my-high-availability-group";
        ProjectRegionInstanceGroupManagerName groupManagerName = ProjectRegionInstanceGroupManagerName.of(instanceGroup,
                MY_AWESOME_PROJECT,
                region);
        String createdBy = groupManagerName.toString();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(createdBy)
                                                                           .withZone(zone)
                                                                           .withPlatform(gcpComputePlatform)
                                                                           .withInstanceName(instanceName)
                                                                           .withUniqueIdentifier(uuid)
                                                                           .build();
        ArgumentCaptor<RegionInstanceGroupManagersRecreateRequest> request = ArgumentCaptor.forClass(
                RegionInstanceGroupManagersRecreateRequest.class);
        String operationSelfLink = "my-operation-self-link";
        Operation operation = Operation.newBuilder().setSelfLink(operationSelfLink).build();
        doReturn(operation).when(regionInstanceGroupManagerClient)
                           .recreateInstancesRegionInstanceGroupManager(any(ProjectRegionInstanceGroupManagerName.class),
                                   request.capture());
        assertEquals(operationSelfLink, gcpComputePlatform.recreateInstanceInInstanceGroup(container));
        assertThat(request.getValue().getInstancesList(),
                containsInAnyOrder(ProjectZoneInstanceName.of(instanceName, MY_AWESOME_PROJECT, zone).toString()));
    }

    @Test
    public void recreateInstanceInZoneInstanceGroup () {
        String uuid = "12345678901234567890";
        String instanceName = "my-awesome-instance";
        String zone = "my-datacenter";
        String instanceGroup = "my-high-availability-group";
        ProjectZoneInstanceGroupManagerName groupManagerName = ProjectZoneInstanceGroupManagerName.of(instanceGroup,
                MY_AWESOME_PROJECT,
                zone);
        String createdBy = groupManagerName.toString();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(createdBy)
                                                                           .withZone(zone)
                                                                           .withPlatform(gcpComputePlatform)
                                                                           .withInstanceName(instanceName)
                                                                           .withUniqueIdentifier(uuid)
                                                                           .build();
        ArgumentCaptor<InstanceGroupManagersRecreateInstancesRequest> request = ArgumentCaptor.forClass(
                InstanceGroupManagersRecreateInstancesRequest.class);
        String operationSelfLink = "my-operation-self-link";
        Operation operation = Operation.newBuilder().setSelfLink(operationSelfLink).build();
        doReturn(operation).when(instanceGroupManagerClient)
                           .recreateInstancesInstanceGroupManager(any(ProjectZoneInstanceGroupManagerName.class),
                                   request.capture());
        assertEquals(operationSelfLink, gcpComputePlatform.recreateInstanceInInstanceGroup(container));
        assertThat(request.getValue().getInstancesList(),
                containsInAnyOrder(ProjectZoneInstanceName.of(instanceName, MY_AWESOME_PROJECT, zone).toString()));
    }

    @Test
    public void getLatestInstanceId () {
        String instanceGivenName = "my-instance";
        String zone = "my-datacenter";
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.of(instanceGivenName, MY_AWESOME_PROJECT, zone);
        String newId = "987654321";
        Instance instance = Instance.newBuilder().setId(newId).build();
        doReturn(instance).when(instanceClient).getInstance(instanceName);
        gcpComputePlatform.getLatestInstanceId(instanceGivenName, zone);
    }

    @Test
    public void setTags () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withZone("my-datacenter")
                                                                           .withInstanceName("my-instance")
                                                                           .withUniqueIdentifier("1234567890")
                                                                           .build();
        List<String> tags = List.of("new-tag");
        String oldFingerprint = "old-fingerprint";
        Tags oldTags = Tags.newBuilder().setFingerprint(oldFingerprint).build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.of("1234567890",
                MY_AWESOME_PROJECT,
                "my-datacenter");
        Instance instance = Instance.newBuilder().setTags(oldTags).build();
        doReturn(instance).when(instanceClient).getInstance(instanceName);
        doReturn("my-operation").when(gcpComputePlatform).setTagsSafe(container, tags, oldFingerprint);
        assertEquals("my-operation", gcpComputePlatform.setTags(container, tags));
    }

    @Test
    public void setTagsSafe () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withZone("my-datacenter")
                                                                           .withInstanceName("my-instance")
                                                                           .withUniqueIdentifier("1234567890")
                                                                           .build();
        List<String> tags = List.of("new-tag");
        String oldFingerprint = "old-fingerprint";
        Tags actualTags = Tags.newBuilder().setFingerprint(oldFingerprint).addAllItems(tags).build();
        ProjectZoneInstanceName instanceName = ProjectZoneInstanceName.of("1234567890",
                MY_AWESOME_PROJECT,
                "my-datacenter");
        doReturn(Operation.newBuilder().setSelfLink("my-operation").build()).when(instanceClient)
                                                                            .setTagsInstance(instanceName, actualTags);
        assertEquals("my-operation", gcpComputePlatform.setTagsSafe(container, tags, oldFingerprint));
    }

    @Test
    public void getFirewallTags () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withUniqueIdentifier("1234567890")
                                                                           .withZone("my-datacenter")
                                                                           .build();
        ProjectZoneInstanceName instanceName = GcpComputePlatform.getProjectZoneInstanceNameOfContainer(container,
                projectName);
        Tags tags = Tags.newBuilder()
                        .setFingerprint("tags-fingerprint")
                        .addAllItems(List.of("HTTP", "HTTPS", "SSH"))
                        .build();
        doReturn(Instance.newBuilder().setTags(tags).build()).when(instanceClient).getInstance(instanceName);
        GcpComputePlatform.Fingerprint<List<String>> expectedTags;
        GcpComputePlatform.Fingerprint<List<String>> actualTags = gcpComputePlatform.getFirewallTags(container);
        expectedTags = new GcpComputePlatform.Fingerprint<>("tags-fingerprint", List.of("HTTP", "HTTPS", "SSH"));
        assertEquals(actualTags, expectedTags);
    }

    @Test
    public void checkTags () {
        GcpComputeInstanceContainer container;
        Tags actualTags = Tags.newBuilder().addAllItems(List.of("HTTP", "HTTPS", "SSH")).build();
        List<String> orderedTags, unorderedTags, differentTags;
        orderedTags = List.of("HTTP", "HTTPS", "SSH");
        unorderedTags = List.of("SSH", "HTTPS", "HTTP");
        differentTags = List.of("RDP", "NTP");
        String instanceName = "0123456789";
        String zone = "my-datacenter";
        container = GcpComputeInstanceContainer.builder().withUniqueIdentifier(instanceName).withZone(zone).build();
        ProjectZoneInstanceName instance = ProjectZoneInstanceName.of(instanceName, MY_AWESOME_PROJECT, zone);
        doReturn(Instance.newBuilder().setTags(actualTags).build()).when(instanceClient).getInstance(instance);
        assertTrue(gcpComputePlatform.checkTags(container, orderedTags));
        assertTrue(gcpComputePlatform.checkTags(container, unorderedTags));
        assertFalse(gcpComputePlatform.checkTags(container, differentTags));
    }

    @Test
    public void waitForOperationSuccess () {
        Operation operation = Operation.newBuilder().setSelfLink("my-operation").build();
        doReturn(false).doReturn(false).doReturn(true).when(gcpComputePlatform).isOperationComplete("my-operation");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        gcpComputePlatform.waitForOperation(operation);
        stopWatch.stop();
        assertThat(stopWatch.getLastTaskTimeMillis(), Matchers.greaterThanOrEqualTo(2000L));
        assertThat(stopWatch.getLastTaskTimeMillis(), Matchers.lessThanOrEqualTo(3999L));
    }

    @Test(expected = ChaosException.class)
    public void waitForOperationInterrupted () {
        Operation operation = Operation.newBuilder().setSelfLink("my-operation").build();
        Thread mainThread = Thread.currentThread();
        doReturn(false).doAnswer(invocationOnMock -> {
            mainThread.interrupt();
            return null;
        }).when(gcpComputePlatform).isOperationComplete("my-operation");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        gcpComputePlatform.waitForOperation(operation);
        stopWatch.stop();
        assertThat(stopWatch.getLastTaskTimeMillis(), Matchers.greaterThanOrEqualTo(1000L));
        assertThat(stopWatch.getLastTaskTimeMillis(), Matchers.lessThanOrEqualTo(2999L));
    }

    @Test
    public void addSSHKey () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withZone("my-datacenter")
                                                                           .withUniqueIdentifier("1234567890")
                                                                           .build();
        Metadata existingMetadata = Metadata.getDefaultInstance();
        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
        Instance instance = Instance.newBuilder().setMetadata(existingMetadata).build();
        Operation operation = mock(Operation.class);
        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        ProjectZoneInstanceName expectedInstance = ProjectZoneInstanceName.of("1234567890",
                MY_AWESOME_PROJECT,
                "my-datacenter");
        doReturn(instance).when(instanceClient).getInstance(expectedInstance);
        doReturn(operation).when(instanceClient).setMetadataInstance(eq(expectedInstance), metadataCaptor.capture());
        doNothing().when(gcpComputePlatform).waitForOperation(any());
        doNothing().when(gcpComputePlatform).addSSHKey(container, false);
        gcpComputePlatform.addSSHKey(container);
        verify(gcpComputePlatform).waitForOperation(operationCaptor.capture());
        String capturedKey = metadataCaptor.getValue()
                                           .getItemsList()
                                           .stream()
                                           .filter(items -> items.getKey().equals("ssh-keys"))
                                           .map(Items::getValue)
                                           .map(String::strip)
                                           .findFirst()
                                           .orElseThrow();
        assertEquals(container.getGcpSSHKeyMetadata().toString(), capturedKey);
        assertSame(operation, operationCaptor.getValue());
        verify(gcpComputePlatform).addSSHKey(container, false);
    }

    @Test
    public void addSSHKeyAlreadyExists () {
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withZone("my-datacenter")
                                                                           .withUniqueIdentifier("1234567890")
                                                                           .build();
        String key = container.getGcpSSHKeyMetadata().toString();
        Metadata existingMetadata = Metadata.newBuilder()
                                            .addItems(Items.newBuilder().setKey("ssh-keys").setValue(key).build())
                                            .build();
        Instance instance = Instance.newBuilder().setMetadata(existingMetadata).build();
        ProjectZoneInstanceName expectedInstance = ProjectZoneInstanceName.of("1234567890",
                MY_AWESOME_PROJECT,
                "my-datacenter");
        doReturn(instance).when(instanceClient).getInstance(expectedInstance);
        doNothing().when(gcpComputePlatform).waitForOperation(any());
        gcpComputePlatform.addSSHKey(container, false);
        verify(instanceClient, never()).setMetadataInstance(any(ProjectZoneInstanceName.class), any());
        verify(gcpComputePlatform, never()).waitForOperation(any());
    }

    @Test
    public void performTaskAfterOperationCompletes () {
        String operationId = "my-operation-id";
        final AtomicBoolean operationComplete = new AtomicBoolean(false);
        doReturn(false, true).when(gcpComputePlatform).isOperationComplete(operationId);
        gcpComputePlatform.performTaskAfterOperationCompletes(operationId, () -> operationComplete.set(true));
        await().atLeast(5500, TimeUnit.MILLISECONDS).atMost(7, TimeUnit.SECONDS).until(operationComplete::get);
    }

    @Configuration
    public static class GcpComputePlatformTestConfiguration {
        @Autowired
        private InstanceClient instanceClient;
        @Autowired
        private InstanceGroupClient instanceGroupClient;
        @Autowired
        private InstanceGroupManagerClient instanceGroupManagerClient;
        @Autowired
        private RegionInstanceGroupClient regionInstanceGroupClient;
        @Autowired
        private RegionInstanceGroupManagerClient regionInstanceGroupManagerClient;
        @Autowired
        private ZoneOperationClient zoneOperationClient;
        @Autowired
        private GcpComputeSelfAwareness selfAwareness;

        @Bean(name = COMPUTE_PROJECT)
        public ProjectName projectName () {
            return ProjectName.of(MY_AWESOME_PROJECT);
        }

        @Bean
        public GcpComputePlatform gcpComputePlatform () {
            return spy(new GcpComputePlatform());
        }
    }
}