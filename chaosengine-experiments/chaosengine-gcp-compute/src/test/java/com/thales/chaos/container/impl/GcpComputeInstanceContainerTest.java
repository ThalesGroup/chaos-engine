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

package com.thales.chaos.container.impl;

import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.impl.GenericContainerExperiment;
import com.thales.chaos.platform.impl.GcpComputePlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class GcpComputeInstanceContainerTest {
    GcpComputeInstanceContainer container;
    @Mock
    GcpComputePlatform platform;
    Experiment experiment;
    String createdBy = "my-instance-group";
    String zone = "my-datacenter";
    String instanceName = "my-apache-server";
    String uniqueIdentifier = "1234567890";
    List<String> firewallTags = List.of("HTTP", "HTTPS", "SSH");

    @Before
    public void setUp () {
        container = spy(GcpComputeInstanceContainer.builder()
                                                   .withUniqueIdentifier(uniqueIdentifier)
                                                   .withInstanceName(instanceName)
                                                   .withZone(zone)
                                                   .withFirewallTags(firewallTags)
                                                   .withCreatedBy(createdBy)
                                                   .withPlatform(platform)
                                                   .build());
        experiment = spy(GenericContainerExperiment.builder().withContainer(container).build());
    }

    @Test
    public void recreateInstanceInGroup () {
        container.recreateInstanceInGroup(experiment);
        verify(platform).recreateInstanceInInstanceGroup(container);
        verify(experiment).setSelfHealingMethod(any());
        verify(experiment).setFinalizeMethod(any());
        verify(experiment).setCheckContainerHealth(any());
    }

    @Test
    public void recreateInstanceInGroupFinalizeMethod () {
        String newUUID = uniqueIdentifier + "0987654321";
        container.recreateInstanceInGroup(experiment);
        doReturn(newUUID).when(platform).getLatestInstanceId(instanceName, zone);
        experiment.getFinalizeMethod().run();
        verify(container).setUniqueIdentifier(newUUID);
    }

    @Test
    public void recreateInstanceInGroupCheckHealthMethodWithNullOperationId () throws Exception {
        container.recreateInstanceInGroup(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform, never()).isOperationComplete(any());
    }

    @Test
    public void recreateInstanceInGroupCheckHealthMethodWithIncompleteOperation () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).recreateInstanceInInstanceGroup(container);
        doReturn(false).when(platform).isOperationComplete(operationId);
        container.recreateInstanceInGroup(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform).isOperationComplete(operationId);
    }

    @Test
    public void recreateInstanceInGroupCheckHealthMethodWithGroupBelowCapacity () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).recreateInstanceInInstanceGroup(container);
        doReturn(true).when(platform).isOperationComplete(operationId);
        doReturn(false).when(platform).isContainerGroupAtCapacity(container);
        container.recreateInstanceInGroup(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform).isContainerGroupAtCapacity(container);
    }

    @Test
    public void recreateInstanceInGroupCheckHealthMethodSuccess () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).recreateInstanceInInstanceGroup(container);
        doReturn(true).when(platform).isOperationComplete(operationId);
        doReturn(true).when(platform).isContainerGroupAtCapacity(container);
        container.recreateInstanceInGroup(experiment);
        assertEquals(ContainerHealth.NORMAL, experiment.getCheckContainerHealth().call());
    }

    @Test
    public void isCattle () {
        GcpComputeInstanceContainer otherContainer = GcpComputeInstanceContainer.builder().build();
        // Validate the null-ness of the CreatedBy value to ensure the tests are accurate
        assertNotNull(container.getAggregationIdentifier());
        assertNull(otherContainer.getAggregationIdentifier());
        assertTrue(container.isCattle());
        assertFalse(otherContainer.isCattle());
    }

    @Test
    public void simulateMaintenance () {
        container.simulateMaintenance(experiment);
        verify(platform).simulateMaintenance(container);
        verify(experiment).setSelfHealingMethod(any());
        verify(experiment).setCheckContainerHealth(any());
    }

    @Test
    public void simulateMaintenanceCheckHealthMethodWithNullOperationId () throws Exception {
        container.simulateMaintenance(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform, never()).isOperationComplete(any());
    }

    @Test
    public void simulateMaintenanceCheckHealthMethodWithIncompleteOperation () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).simulateMaintenance(container);
        doReturn(false).when(platform).isOperationComplete(operationId);
        container.simulateMaintenance(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform).isOperationComplete(operationId);
    }

    @Test
    public void simulateMaintenanceCheckHealthMethodSuccess () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).simulateMaintenance(container);
        doReturn(true).when(platform).isOperationComplete(operationId);
        container.simulateMaintenance(experiment);
        assertEquals(ContainerHealth.NORMAL, experiment.getCheckContainerHealth().call());
    }

    @Test
    public void stopInstance () {
        container.stopInstance(experiment);
        verify(platform).stopInstance(container);
        verify(experiment).setSelfHealingMethod(any());
        verify(experiment).setCheckContainerHealth(any());
    }

    @Test
    public void stopInstanceSelfHealing () {
        container.stopInstance(experiment);
        experiment.getSelfHealingMethod().run();
        verify(platform).startInstance(container);
    }

    @Test
    public void stopInstanceCheckHealthNullOperation () throws Exception {
        container.stopInstance(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform, never()).isOperationComplete(any());
    }

    @Test
    public void stopInstanceCheckHealthIncompleteOperation () throws Exception {
        String operationId = "my-operation";
        doReturn(operationId).when(platform).stopInstance(container);
        doReturn(false).when(platform).isOperationComplete(operationId);
        container.stopInstance(experiment);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        verify(platform).isOperationComplete(operationId);
        verify(platform, never()).isContainerRunning(container);
    }

    @Test
    public void stopInstanceCheckHealthCompleteOperation () throws Exception {
        String operationId = "my-operation";
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            doReturn(operationId).when(platform).stopInstance(container);
            doReturn(true).when(platform).isOperationComplete(operationId);
            doReturn(containerHealth).when(platform).isContainerRunning(container);
            container.stopInstance(experiment);
            assertEquals(containerHealth, experiment.getCheckContainerHealth().call());
        }
    }
}