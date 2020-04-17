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

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpSqlContainer;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.services.impl.GcpCredentialsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.thales.chaos.platform.impl.GcpSqlPlatform.SQL_FAILOVER_CONTEXT;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class GcpSqlPlatformTest {
    private static final String PROJECT_NAME = "gcp-project";
    @MockBean
    private GoogleCredentials googleCredentials;
    @MockBean
    private ContainerManager containerManager;
    @Autowired
    private GcpSqlPlatform platform;
    @Autowired
    private GcpCredentialsMetadata gcpCredentialsMetadata;
    private DatabaseInstance nonEligibleInstance;
    private DatabaseInstance noHaInstance;
    private DatabaseInstance haInstanceNoReplicas;
    private DatabaseInstance haInstanceWithReplicas;
    private DatabaseInstance haInstanceExcludeLabels;
    private DatabaseInstance replicaInstance;
    private DatabaseInstance startingReplicaInstance;
    private List<DatabaseInstance> allInstances;
    private Map<String, String> includeFilterLabels = Map.of("chaos", "yes");
    private Map<String, String> excludeFilterLabels = Map.of("chaos", "no");

    @Before
    public void setUp () {
        Settings includeSettings = new Settings();
        includeSettings.setUserLabels(includeFilterLabels);
        Settings excludeSettings = new Settings();
        excludeSettings.setUserLabels(excludeFilterLabels);
        nonEligibleInstance = new DatabaseInstance().setName("nonEligibleInstance")
                                                    .setState("FAILED")
                                                    .setFailoverReplica(new DatabaseInstance.FailoverReplica());
        noHaInstance = new DatabaseInstance().setName("NoHaInstance")
                                             .setFailoverReplica(null)
                                             .setState(GcpSqlPlatform.SQL_INSTANCE_RUNNING);
        haInstanceNoReplicas = new DatabaseInstance().setName("HaInstanceNoReplicas")
                                                     .setFailoverReplica(new DatabaseInstance.FailoverReplica())
                                                     .setState(GcpSqlPlatform.SQL_INSTANCE_RUNNING)
                                                     .setSettings(includeSettings)
                                                     .setReplicaNames(Collections.emptyList());
        haInstanceWithReplicas = new DatabaseInstance().setName("HaInstanceWithReplicas")
                                                       .setFailoverReplica(new DatabaseInstance.FailoverReplica())
                                                       .setState(GcpSqlPlatform.SQL_INSTANCE_RUNNING)
                                                       .setSettings(includeSettings);
        haInstanceExcludeLabels = new DatabaseInstance().setName("haInstanceExcludeLabels")
                                                        .setFailoverReplica(new DatabaseInstance.FailoverReplica())
                                                        .setState(GcpSqlPlatform.SQL_INSTANCE_RUNNING)
                                                        .setSettings(excludeSettings);
        replicaInstance = new DatabaseInstance().setName("ReplicaInstance")
                                                .setFailoverReplica(null)
                                                .setMasterInstanceName(haInstanceWithReplicas.getName())
                                                .setState(GcpSqlPlatform.SQL_INSTANCE_RUNNING);
        startingReplicaInstance = new DatabaseInstance().setName("StartingReplica")
                                                        .setFailoverReplica(null)
                                                        .setMasterInstanceName(haInstanceWithReplicas.getName())
                                                        .setState("STARTING");
        haInstanceWithReplicas.setReplicaNames(List.of(replicaInstance.getName(), startingReplicaInstance.getName()));
        allInstances = List.of(nonEligibleInstance,
                noHaInstance,
                haInstanceNoReplicas,
                haInstanceWithReplicas,
                haInstanceExcludeLabels,
                replicaInstance,
                startingReplicaInstance);
    }

    @Test
    public void getApiStatus () throws IOException {
        doReturn(null).doThrow(RuntimeException.class).when(platform).getInstances();
        assertThat(platform.getApiStatus(), is(ApiStatus.OK));
        assertThat(platform.getApiStatus(), is(ApiStatus.ERROR));
    }

    @Test
    public void getPlatformHealth () throws IOException {
        List<DatabaseInstance> instanceList = List.of(new DatabaseInstance());
        doReturn(instanceList).doReturn(List.of()).doThrow(RuntimeException.class).when(platform).getInstances();
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.OK));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.DEGRADED));
        assertThat(platform.getPlatformHealth(), is(PlatformHealth.FAILED));
    }

    @Test
    public void getMasterInstances () throws IOException {
        doReturn(allInstances).when(platform).getInstances();
        List<DatabaseInstance> instances = platform.getMasterInstances();
        assertThat(instances,
                containsInAnyOrder(haInstanceNoReplicas, haInstanceWithReplicas, haInstanceExcludeLabels));
    }

    @Test
    public void getMasterInstancesFailed () throws IOException {
        GcpSqlPlatform gcpSqlPlatform = spy(new GcpSqlPlatform());
        doThrow(IOException.class).when(gcpSqlPlatform).getInstances();
        List<DatabaseInstance> instances = gcpSqlPlatform.getMasterInstances();
        assertThat(instances, is(empty()));
    }

    @Test
    public void getReadReplicas () throws IOException {
        doReturn(allInstances).when(platform).getInstances();
        List<DatabaseInstance> instances = platform.getReadReplicas(haInstanceWithReplicas);
        assertThat(instances, containsInAnyOrder(replicaInstance, startingReplicaInstance));
        assertThat(platform.getReadReplicas(haInstanceNoReplicas), is(Collections.emptyList()));
    }

    @Test
    public void getReadReplicasFailed () throws IOException {
        GcpSqlPlatform gcpSqlPlatform = spy(new GcpSqlPlatform());
        doThrow(IOException.class).when(gcpSqlPlatform).getInstances();
        List<DatabaseInstance> instances = gcpSqlPlatform.getReadReplicas(haInstanceWithReplicas);
        assertThat(instances, is(empty()));
    }

    @Test
    public void generateRoster () throws IOException {
        doReturn(allInstances).when(platform).getInstances();
        List<Container> roster = platform.generateRoster();
        assertThat(roster,
                containsInAnyOrder(platform.createContainerFromInstance(haInstanceNoReplicas),
                        platform.createContainerFromInstance(haInstanceWithReplicas),
                        platform.createContainerFromInstance(haInstanceExcludeLabels)));
        assertThat(roster.size(), is(3));
    }

    @Test
    public void generateRosterFiltersSet () throws IOException {
        GcpSqlPlatform gcpSqlPlatform = spy(new GcpSqlPlatform());
        gcpSqlPlatform.setIncludeFilter(includeFilterLabels);
        gcpSqlPlatform.setExcludeFilter(excludeFilterLabels);
        doReturn(allInstances).when(gcpSqlPlatform).getInstances();
        List<Container> roster = gcpSqlPlatform.generateRoster();
        assertThat(roster,
                containsInAnyOrder(platform.createContainerFromInstance(haInstanceNoReplicas),
                        platform.createContainerFromInstance(haInstanceWithReplicas)));
        assertThat(roster.size(), is(2));
    }

    @Test
    public void isContainerRunning () {
        doReturn(null).doReturn(haInstanceNoReplicas)
                      .doReturn(haInstanceWithReplicas)
                      .doReturn(haInstanceWithReplicas)
                      .doReturn(nonEligibleInstance)
                      .when(platform)
                      .getInstance(anyString());
        // Instance not found
        assertThat(platform.isContainerRunning(platform.createContainerFromInstance(haInstanceNoReplicas)),
                is(ContainerHealth.DOES_NOT_EXIST));
        // OK instance with no read replicas
        assertThat(platform.isContainerRunning(platform.createContainerFromInstance(haInstanceNoReplicas)),
                is(ContainerHealth.NORMAL));
        doReturn(List.of(replicaInstance)).doReturn(List.of(replicaInstance, startingReplicaInstance))
                                          .when(platform)
                                          .getReadReplicas(ArgumentMatchers.any(DatabaseInstance.class));
        // Cluster with read a replica
        assertThat(platform.isContainerRunning(platform.createContainerFromInstance(haInstanceWithReplicas)),
                is(ContainerHealth.NORMAL));
        // Cluster with one starting replica
        assertThat(platform.isContainerRunning(platform.createContainerFromInstance(haInstanceWithReplicas)),
                is(ContainerHealth.RUNNING_EXPERIMENT));
        // Master is not running
        assertThat(platform.isContainerRunning(platform.createContainerFromInstance(nonEligibleInstance)),
                is(ContainerHealth.RUNNING_EXPERIMENT));
    }

    @Test
    public void failover () throws IOException {
        GcpSqlContainer gcpSqlContainer = platform.createContainerFromInstance(haInstanceWithReplicas);
        SQLAdmin sqlAdmin = mock(SQLAdmin.class);
        SQLAdmin.Instances instances = mock(SQLAdmin.Instances.class);
        SQLAdmin.Instances.Failover failover = mock(SQLAdmin.Instances.Failover.class);
        Operation operation = mock(Operation.class);
        ArgumentCaptor<InstancesFailoverRequest> instancesFailoverRequestArgumentCaptor = ArgumentCaptor.forClass(
                InstancesFailoverRequest.class);
        ArgumentCaptor<String> instanceNameCaptor = ArgumentCaptor.forClass(String.class);
        InstancesFailoverRequest instancesFailoverRequest = new InstancesFailoverRequest();
        instancesFailoverRequest.setFailoverContext(new FailoverContext().setSettingsVersion(haInstanceWithReplicas.getSettings()
                                                                                                                   .getSettingsVersion())
                                                                         .setKind(SQL_FAILOVER_CONTEXT));
        doReturn(haInstanceWithReplicas).when(platform).getInstance(haInstanceWithReplicas.getName());
        doReturn(sqlAdmin).when(platform).getSQLAdmin();
        doReturn(instances).when(sqlAdmin).instances();
        doReturn(failover).when(instances)
                          .failover(any(),
                                  instanceNameCaptor.capture(),
                                  instancesFailoverRequestArgumentCaptor.capture());
        doReturn(operation).when(failover).execute();
        platform.failover(gcpSqlContainer);
        verify(failover).execute();
        assertThat(instanceNameCaptor.getValue(), is(haInstanceWithReplicas.getName()));
        assertThat(instancesFailoverRequestArgumentCaptor.getValue(), is(instancesFailoverRequest));
    }

    @Test
    public void getPlatformLevel () {
        assertThat(platform.getPlatformLevel(), is(PlatformLevel.IAAS));
    }

    @Configuration
    public static class GcpSqlPlatformTestConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private GoogleCredentials googleCredentials;

        @Bean
        public GcpSqlPlatform gcpSqlPlatform () {
            return spy(new GcpSqlPlatform());
        }

        @Bean
        public GcpCredentialsMetadata gcpCredentialsMetadata () {
            return new GcpCredentialsMetadata(PROJECT_NAME);
        }
    }
}