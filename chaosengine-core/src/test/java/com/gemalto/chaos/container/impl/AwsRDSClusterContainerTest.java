package com.gemalto.chaos.container.impl;

import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AwsRDSClusterContainerTest {
    private static final String DB_CLUSTER_RESOURCE_ID = UUID.randomUUID().toString();
    private AwsRDSClusterContainer awsRDSClusterContainer;
    @MockBean
    private AwsRDSPlatform awsRDSPlatform;
    private String dbClusterIdentifier = UUID.randomUUID().toString();
    private String engine = UUID.randomUUID().toString();

    @Before
    public void setUp () {
        awsRDSClusterContainer = Mockito.spy(AwsRDSClusterContainer.builder()
                                                                   .withAwsRDSPlatform(awsRDSPlatform)
                                                                   .withDbClusterIdentifier(dbClusterIdentifier)
                                                                   .withEngine(engine)
                                                                   .withDBClusterResourceId(DB_CLUSTER_RESOURCE_ID)
                                                                   .build());
    }

    @Test
    public void getAggegationIdentifier () {
        assertEquals(dbClusterIdentifier, awsRDSClusterContainer.getAggregationIdentifier());
    }

    @Test
    public void getDbClusterIdentifier () {
        assertEquals(dbClusterIdentifier, awsRDSClusterContainer.getDbClusterIdentifier());
    }

    @Test
    public void supportsShellBasedExperiments () {
        assertFalse(awsRDSClusterContainer.supportsShellBasedExperiments());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsRDSPlatform, awsRDSClusterContainer.getPlatform());
    }

    @Test
    public void getSimpleName () {
        assertEquals(dbClusterIdentifier, awsRDSClusterContainer.getSimpleName());
    }

    @Test
    public void getMembers () {
        awsRDSClusterContainer.getMembers();
        verify(awsRDSPlatform, times(1)).getClusterInstances(dbClusterIdentifier);
    }

    @Test
    @Repeat(value = 10)
    @SuppressWarnings("unchecked")
    public void getSomeMembers () {
        Set<String> members = new HashSet<>();
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();
        members.add(instanceId1);
        members.add(instanceId2);
        when(awsRDSClusterContainer.getMembers()).thenReturn(members);
        assertThat(awsRDSClusterContainer.getSomeMembers(), isOneOf(Collections.singleton(instanceId1), Collections.singleton(instanceId2)));
        String instanceId3 = UUID.randomUUID().toString();
        members.add(instanceId3);
        Set<Set<String>> validSets;
        Set<String> set12 = new HashSet<>(Arrays.asList(instanceId1, instanceId2));
        Set<String> set21 = new HashSet<>(Arrays.asList(instanceId2, instanceId1));
        Set<String> set13 = new HashSet<>(Arrays.asList(instanceId1, instanceId3));
        Set<String> set31 = new HashSet<>(Arrays.asList(instanceId1, instanceId1));
        Set<String> set23 = new HashSet<>(Arrays.asList(instanceId2, instanceId3));
        Set<String> set32 = new HashSet<>(Arrays.asList(instanceId3, instanceId2));
        validSets = new HashSet<>(Arrays.asList(set12, set21, set13, set31, set23, set32, Collections.singleton(instanceId1), Collections
                .singleton(instanceId2), Collections.singleton(instanceId3)));
        assertThat(awsRDSClusterContainer.getSomeMembers(), isIn(validSets));
    }

    @Test(expected = ChaosException.class)
    public void getSomeMembersException () {
        Set<String> members = Collections.singleton("Single Member");
        doReturn(members).when(awsRDSPlatform).getClusterInstances(dbClusterIdentifier);
        awsRDSClusterContainer.getSomeMembers();
    }

    @Test
    public void restartInstances () {
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();
        Experiment experiment = mock(Experiment.class);
        Set<String> members = new HashSet<>(Arrays.asList(instanceId1, instanceId2));
        doReturn(members).when(awsRDSClusterContainer).getSomeMembers();
        awsRDSClusterContainer.restartInstances(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(any());
        verify(awsRDSPlatform, times(1)).restartInstance(members.toArray(new String[0]));
    }

    @Test
    public void initiateFailover () {
        Experiment experiment = mock(Experiment.class);
        doReturn(null).when(awsRDSClusterContainer).getSomeMembers();
        awsRDSClusterContainer.initiateFailover(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(any());
        verify(awsRDSPlatform, times(1)).failoverCluster(dbClusterIdentifier);
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier()
                                      .withKey("dbclusteridentifier")
                                      .withValue(dbClusterIdentifier), awsRDSClusterContainer.getDataDogIdentifier());
    }

    @Test
    public void snapshotCluster () throws Exception {
        Experiment experiment = spy(Experiment.class);
        DBClusterSnapshot dbClusterSnapshot = mock(DBClusterSnapshot.class);
        doReturn(dbClusterSnapshot).when(awsRDSPlatform).snapshotDBCluster(dbClusterIdentifier);
        awsRDSClusterContainer.startSnapshot(experiment);
        verify(awsRDSPlatform, times(1)).snapshotDBCluster(dbClusterIdentifier);
        verify(experiment, times(1)).setFinalizeMethod(any());
        verify(experiment, times(1)).setSelfHealingMethod(any());
        verify(experiment, times(1)).setCheckContainerHealth(any());
        experiment.getSelfHealingMethod().call();
        verify(awsRDSPlatform, times(1)).deleteClusterSnapshot(dbClusterSnapshot);
        reset(awsRDSPlatform);
        experiment.getFinalizeMethod().call();
        verify(awsRDSPlatform, times(1)).deleteClusterSnapshot(dbClusterSnapshot);
        reset(awsRDSPlatform);
        doReturn(true, false).when(awsRDSPlatform).isClusterSnapshotRunning(dbClusterIdentifier);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        assertEquals(ContainerHealth.NORMAL, experiment.getCheckContainerHealth().call());
    }

    @Test
    public void dataDogTags () {
        Map<String, String> baseContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        awsRDSClusterContainer.setMappedDiagnosticContext();
        Map<String, String> modifiedContextMap = MDC.getCopyOfContextMap();
        awsRDSClusterContainer.clearMappedDiagnosticContext();
        Map<String, String> finalContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, DB_CLUSTER_RESOURCE_ID);
        expectedTags.put(AwsRDSConstants.AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier);
        expectedTags.putAll(baseContextMap);
        assertEquals(baseContextMap, finalContextMap);
        assertEquals(expectedTags, modifiedContextMap);
    }

}