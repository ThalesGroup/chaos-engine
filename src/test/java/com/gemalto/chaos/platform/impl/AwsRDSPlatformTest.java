package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.util.AwsRDSUtils;
import com.gemalto.chaos.util.CalendarUtils;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.AwsRDSConstants.*;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AwsRDSPlatformTest {
    @MockBean
    private AmazonRDS amazonRDS;
    @MockBean
    private AmazonEC2 amazonEC2;
    @SpyBean
    private ContainerManager containerManager;
    @Autowired
    private AwsRDSPlatform awsRDSPlatform;

    @Test
    public void getApiStatus () {
        // Case 1: Both return, return normal
        when(amazonRDS.describeDBInstances()).thenReturn(null);
        when(amazonRDS.describeDBClusters()).thenReturn(null);
        assertEquals(ApiStatus.OK, awsRDSPlatform.getApiStatus());
        Mockito.reset(amazonRDS);
        // Case 2: First call errors, return error.
        when(amazonRDS.describeDBInstances()).thenThrow(mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, awsRDSPlatform.getApiStatus());
        Mockito.reset(amazonRDS);
        // Case 3: First call succeeds, second call errors, return error.
        when(amazonRDS.describeDBInstances()).thenReturn(null);
        when(amazonRDS.describeDBClusters()).thenThrow(mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, awsRDSPlatform.getApiStatus());
    }

    @Test
    public void getPlatformLevel () {
        assertEquals(PlatformLevel.IAAS, awsRDSPlatform.getPlatformLevel());
    }

    @Test
    public void getPlatformHealth () {
        DBInstance normalDbInstance = new DBInstance().withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE);
        DBInstance failedDbInstance = new DBInstance().withDBInstanceStatus(AwsRDSConstants.AWS_RDS_FAILING_OVER);
        DescribeDBInstancesResult describeDBInstancesResult;
        // Case 1: Single normal instance.
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(normalDbInstance);
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.OK, awsRDSPlatform.getPlatformHealth());
        // Case 2: Many normal instances
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(normalDbInstance.clone(), normalDbInstance
                .clone(), normalDbInstance.clone());
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.OK, awsRDSPlatform.getPlatformHealth());
        // Case 3: Mix of Normal and Failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(normalDbInstance.clone(), normalDbInstance
                .clone(), failedDbInstance.clone(), failedDbInstance.clone());
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.DEGRADED, awsRDSPlatform.getPlatformHealth());
        // Case 4: One failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(failedDbInstance.clone());
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.FAILED, awsRDSPlatform.getPlatformHealth());
        // Case 5: Many failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(failedDbInstance.clone(), failedDbInstance
                .clone(), failedDbInstance.clone());
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.FAILED, awsRDSPlatform.getPlatformHealth());
    }

    @Test
    public void generateRoster () {
        String dbInstance1Identifier = randomUUID().toString();
        String dbInstance2Identifier = randomUUID().toString();
        String instance1AvailabilityZone = randomUUID().toString();
        String instance2AvailabilityZone = randomUUID().toString();
        String dbCluster1Identifier = randomUUID().toString();
        String dbCluster2Identifier = randomUUID().toString();
        String dbClusterInstanceIdentifier = randomUUID().toString();
        DBInstance dbInstance1 = new DBInstance().withDBInstanceIdentifier(dbInstance1Identifier)
                                                 .withDBClusterIdentifier(null)
                                                 .withAvailabilityZone(instance1AvailabilityZone);
        DBInstance dbInstance2 = new DBInstance().withDBInstanceIdentifier(dbInstance2Identifier)
                                                 .withDBClusterIdentifier(null)
                                                 .withAvailabilityZone(instance2AvailabilityZone);
        DBInstance clusterInstance = new DBInstance().withDBInstanceIdentifier(dbClusterInstanceIdentifier)
                                                     .withMultiAZ(true)
                                                     .withDBClusterIdentifier(dbCluster1Identifier);
        DBCluster dbCluster1 = new DBCluster().withDBClusterIdentifier(dbCluster1Identifier);
        DBCluster dbCluster2 = new DBCluster().withDBClusterIdentifier(dbCluster2Identifier);
        String marker1 = randomUUID().toString();
        String marker2 = randomUUID().toString();
        doReturn(new DescribeDBInstancesResult().withDBInstances(dbInstance1, dbInstance2, clusterInstance)
                                                .withMarker(marker1)).when(amazonRDS)
                                                                     .describeDBInstances(new DescribeDBInstancesRequest());
        doReturn(new DescribeDBClustersResult().withDBClusters(dbCluster1, dbCluster2)
                                               .withMarker(marker2)).when(amazonRDS)
                                                                    .describeDBClusters(new DescribeDBClustersRequest());
        doReturn(new DescribeDBInstancesResult()).when(amazonRDS)
                                                 .describeDBInstances(new DescribeDBInstancesRequest().withMarker(marker1));
        doReturn(new DescribeDBClustersResult()).when(amazonRDS)
                                                .describeDBClusters(new DescribeDBClustersRequest().withMarker(marker2));
        assertThat(awsRDSPlatform.generateRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(AwsRDSClusterContainer
                .builder()
                .withDbClusterIdentifier(dbCluster1Identifier)
                .withAwsRDSPlatform(awsRDSPlatform)
                .build(), AwsRDSClusterContainer.builder()
                                                .withDbClusterIdentifier(dbCluster2Identifier)
                                                .withAwsRDSPlatform(awsRDSPlatform)
                                                .build(), AwsRDSInstanceContainer.builder()
                                                                                 .withAwsRDSPlatform(awsRDSPlatform)
                                                                                 .withDbInstanceIdentifier(dbInstance1Identifier)
                                                                                 .withAvailabilityZone(instance1AvailabilityZone)
                                                                                 .build(), AwsRDSInstanceContainer.builder()
                                                                                                                  .withAwsRDSPlatform(awsRDSPlatform)
                                                                                                                  .withDbInstanceIdentifier(dbInstance2Identifier)
                                                                                                                  .withAvailabilityZone(instance2AvailabilityZone)
                                                                                                                  .build()));
    }

    @Test
    public void getDBInstanceHealth () {
        AwsRDSInstanceContainer awsRDSInstanceContainer = mock(AwsRDSInstanceContainer.class);
        doReturn(new DescribeDBInstancesResult().withDBInstances()).when(amazonRDS)
                                                                   .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));
        doReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceStatus("Bogus"))).when(amazonRDS)
                                                                                                                 .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));
        doReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE)))
                .when(amazonRDS)
                .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));




    }

    @Test
    public void getDBClusterHealth () {
        String memberId1 = randomUUID().toString();
        String memberId2 = randomUUID().toString();
        AwsRDSClusterContainer awsRDSClusterContainer = mock(AwsRDSClusterContainer.class);
        doReturn(new DescribeDBClustersResult().withDBClusters()).when(amazonRDS)
                                                                 .describeDBClusters(any(DescribeDBClustersRequest.class));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
        doReturn(new DescribeDBClustersResult().withDBClusters(new DBCluster().withStatus(AwsRDSConstants.AWS_RDS_FAILING_OVER)))
                .when(amazonRDS)
                .describeDBClusters(any(DescribeDBClustersRequest.class));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
        doReturn(new DescribeDBClustersResult().withDBClusters(new DBCluster().withStatus(AwsRDSConstants.AWS_RDS_AVAILABLE)))
                .when(amazonRDS)
                .describeDBClusters(any(DescribeDBClustersRequest.class));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
        doReturn(new DescribeDBClustersResult().withDBClusters(new DBCluster().withStatus(AwsRDSConstants.AWS_RDS_AVAILABLE)
                                                                              .withDBClusterMembers(new DBClusterMember()
                                                                                      .withDBInstanceIdentifier(memberId1), new DBClusterMember()
                                                                                      .withDBInstanceIdentifier(memberId2))))
                .when(amazonRDS)
                .describeDBClusters(any(DescribeDBClustersRequest.class));
        doReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE)))
                .when(amazonRDS)
                .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
    }

    @Test
    public void failoverCluster () {
        String clusterIdentifier = randomUUID().toString();
        awsRDSPlatform.failoverCluster(clusterIdentifier);
        verify(amazonRDS, times(1)).failoverDBCluster(new FailoverDBClusterRequest().withDBClusterIdentifier(clusterIdentifier));
    }

    @Test
    public void restartInstance () {
        String instanceId1 = randomUUID().toString();
        String instanceId2 = randomUUID().toString();
        awsRDSPlatform.restartInstance(instanceId1);
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId1));
        Mockito.reset(amazonRDS);
        awsRDSPlatform.restartInstance(instanceId1, instanceId2);
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId1));
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId2));
    }

    @Test
    public void getClusterInstances () {
        String instanceId1 = randomUUID().toString();
        String instanceId2 = randomUUID().toString();
        String clusterId = randomUUID().toString();
        DescribeDBClustersResult dbClustersResult = new DescribeDBClustersResult().withDBClusters(new DBCluster().withDBClusterMembers(new DBClusterMember()
                .withDBInstanceIdentifier(instanceId1), new DBClusterMember().withDBInstanceIdentifier(instanceId2)));
        doReturn(dbClustersResult).when(amazonRDS)
                                  .describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(clusterId));
        assertThat(awsRDSPlatform.getClusterInstances(clusterId), IsIterableContainingInAnyOrder.containsInAnyOrder(instanceId1, instanceId2));
    }

    @Test
    public void getInstanceStatus () {
        String instanceId1 = randomUUID().toString();
        String instanceId2 = randomUUID().toString();
        DescribeDBInstancesResult normalInstance1 = new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceIdentifier(instanceId1)
                                                                                                                    .withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE));
        DescribeDBInstancesResult normalInstance2 = new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceIdentifier(instanceId2)
                                                                                                                    .withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE));
        DescribeDBInstancesResult abnormalInstance1 = new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceIdentifier(instanceId1)
                                                                                                                      .withDBInstanceStatus(AwsRDSConstants.AWS_RDS_FAILING_OVER));
        DescribeDBInstancesResult abnormalInstance2 = new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceIdentifier(instanceId2)
                                                                                                                      .withDBInstanceStatus(AwsRDSConstants.AWS_RDS_FAILING_OVER));
        DescribeDBInstancesResult emptyDBInstanceresult = new DescribeDBInstancesResult();
        doReturn(emptyDBInstanceresult).when(amazonRDS)
                                       .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId1));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsRDSPlatform.getInstanceStatus(instanceId1));
        reset(amazonRDS);
        doReturn(normalInstance1).when(amazonRDS)
                                 .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId1));
        doReturn(normalInstance2).when(amazonRDS)
                                 .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId2));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.getInstanceStatus(instanceId1, instanceId2));
        reset(amazonRDS);
        doReturn(normalInstance1).when(amazonRDS)
                                 .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId1));
        doReturn(abnormalInstance2).when(amazonRDS)
                                   .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId2));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.getInstanceStatus(instanceId1, instanceId2));
        reset(amazonRDS);
        doReturn(abnormalInstance1).when(amazonRDS)
                                   .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId1));
        doReturn(normalInstance2).when(amazonRDS)
                                 .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId2));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.getInstanceStatus(instanceId1, instanceId2));
    }

    @Test
    public void getVpcSecurityGroupIds () {
        String dbInstanceIdentifier = randomUUID().toString();
        Collection<String> expectedVpcIds = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            expectedVpcIds.add(randomUUID().toString());
        }
        when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier)))
                .thenReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withVpcSecurityGroups(expectedVpcIds
                        .stream()
                        .map(vpcIds -> new VpcSecurityGroupMembership().withVpcSecurityGroupId(vpcIds))
                        .collect(Collectors.toSet()))));
        assertThat(awsRDSPlatform.getVpcSecurityGroupIds(dbInstanceIdentifier), IsIterableContainingInAnyOrder.containsInAnyOrder(expectedVpcIds
                .toArray()));
    }

    @Test
    public void setVpcSecurityGroupIds () {
        String dbInstanceIdentifier = randomUUID().toString();
        String vpcSecurityGroupId = randomUUID().toString();
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupId);
        verify(amazonRDS, times(1)).modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(Collections
                .singleton(vpcSecurityGroupId)));
    }

    @Test
    public void setVpcSecurityGroupIds1 () {
        String dbInstanceIdentifier = randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            vpcSecurityGroupIds.add(randomUUID().toString());
        }
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds);
        verify(amazonRDS, times(1)).modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(vpcSecurityGroupIds));
    }

    @Test
    public void checkVpcSecurityGroupIds () {
        String dbInstanceIdentifier = randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        vpcSecurityGroupIds.add(randomUUID().toString());
        when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier)))
                .thenReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withVpcSecurityGroups(vpcSecurityGroupIds
                        .stream()
                        .map(vpcIds -> new VpcSecurityGroupMembership().withVpcSecurityGroupId(vpcIds))
                        .collect(Collectors.toSet()))));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds
                .iterator()
                .next()));
    }

    @Test
    public void checkVpcSecurityGroupIds1 () {
        String dbInstanceIdentifier = randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        List<String> reverseOrderSet = new ArrayList<>();
        for (int x = 0; x < 100; x++) {
            String randomString = randomUUID().toString();
            vpcSecurityGroupIds.add(randomString);
            reverseOrderSet.add(randomString);
        }
        Collections.reverse(reverseOrderSet);
        Collection<String> biggerSet = new HashSet<>(vpcSecurityGroupIds);
        biggerSet.add(randomUUID().toString());
        Collection<String> smallerSet = new HashSet<>(vpcSecurityGroupIds);
        smallerSet.remove(vpcSecurityGroupIds.iterator().next());
        when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier)))
                .thenReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withVpcSecurityGroups(vpcSecurityGroupIds
                        .stream()
                        .map(vpcIds -> new VpcSecurityGroupMembership().withVpcSecurityGroupId(vpcIds))
                        .collect(Collectors.toSet()))));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds
                .iterator()
                .next()));
        // Two equal sets
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds));
        // Second set is in different order
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, reverseOrderSet));
        // Second set has one more item
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, biggerSet));
        // Second set has one less item
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, smallerSet));
    }

    @Test
    public void getChaosSecurityGroup () {
        String defaultVpcId = randomUUID().toString();
        String customVpcId = randomUUID().toString();
        String defaultSecurityGroupID = randomUUID().toString();
        String customSecurityGroupID = randomUUID().toString();
        String chaosSecurityGroupID = randomUUID().toString();
        SecurityGroup chaosSecurityGroup = new SecurityGroup().withGroupName(AWS_RDS_CHAOS_SECURITY_GROUP)
                                                              .withVpcId(defaultVpcId)
                                                              .withGroupId(chaosSecurityGroupID);
        SecurityGroup defaultSecurityGroup = new SecurityGroup().withGroupName("Default")
                                                                .withVpcId(defaultVpcId)
                                                                .withGroupId(defaultSecurityGroupID);
        SecurityGroup customSecurityGroup = new SecurityGroup().withGroupName("Custom")
                                                               .withVpcId(customVpcId)
                                                               .withGroupId(customSecurityGroupID);
        doReturn(new DescribeSecurityGroupsResult().withSecurityGroups(chaosSecurityGroup, defaultSecurityGroup, customSecurityGroup))
                .when(amazonEC2)
                .describeSecurityGroups();
        assertEquals(chaosSecurityGroupID, awsRDSPlatform.getChaosSecurityGroup());
        assertEquals(chaosSecurityGroupID, awsRDSPlatform.getChaosSecurityGroup());
        verify(awsRDSPlatform, times(1)).initChaosSecurityGroup();
    }

    @Test
    public void getChaosSecurityGroup2 () {
        String defaultVpcId = randomUUID().toString();
        String customVpcId = randomUUID().toString();
        String defaultSecurityGroupID = randomUUID().toString();
        String customSecurityGroupID = randomUUID().toString();
        String chaosSecurityGroupID = randomUUID().toString();
        SecurityGroup defaultSecurityGroup = new SecurityGroup().withGroupName("Default")
                                                                .withVpcId(defaultVpcId)
                                                                .withGroupId(defaultSecurityGroupID);
        SecurityGroup customSecurityGroup = new SecurityGroup().withGroupName("Custom")
                                                               .withVpcId(customVpcId)
                                                               .withGroupId(customSecurityGroupID);
        Vpc defaultVpc = new Vpc().withIsDefault(true).withVpcId(defaultVpcId);
        Vpc customVpc = new Vpc().withIsDefault(false).withVpcId(customVpcId);
        doReturn(new DescribeSecurityGroupsResult().withSecurityGroups(defaultSecurityGroup, customSecurityGroup)).when(amazonEC2)
                                                                                                                  .describeSecurityGroups();
        doReturn(new DescribeVpcsResult().withVpcs(defaultVpc, customVpc)).when(amazonEC2).describeVpcs();
        doReturn(new CreateSecurityGroupResult().withGroupId(chaosSecurityGroupID)).when(amazonEC2)
                                                                                   .createSecurityGroup(new CreateSecurityGroupRequest()
                                                                                           .withGroupName(AWS_RDS_CHAOS_SECURITY_GROUP)
                                                                                           .withVpcId(defaultVpcId)
                                                                                           .withDescription(AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION));
        assertEquals(chaosSecurityGroupID, awsRDSPlatform.getChaosSecurityGroup());
    }

    @Test(expected = ChaosException.class)
    public void getChaosSecurityGroup3 () {
        String defaultVpcId = randomUUID().toString();
        String customVpcId = randomUUID().toString();
        String defaultSecurityGroupID = randomUUID().toString();
        String customSecurityGroupID = randomUUID().toString();
        String chaosSecurityGroupID = randomUUID().toString();
        SecurityGroup defaultSecurityGroup = new SecurityGroup().withGroupName("Default")
                                                                .withVpcId(defaultVpcId)
                                                                .withGroupId(defaultSecurityGroupID);
        SecurityGroup customSecurityGroup = new SecurityGroup().withGroupName("Custom")
                                                               .withVpcId(customVpcId)
                                                               .withGroupId(customSecurityGroupID);
        Vpc customVpc = new Vpc().withIsDefault(false).withVpcId(customVpcId);
        doReturn(new DescribeSecurityGroupsResult().withSecurityGroups(defaultSecurityGroup, customSecurityGroup)).when(amazonEC2)
                                                                                                                  .describeSecurityGroups();
        doReturn(new DescribeVpcsResult().withVpcs(customVpc)).when(amazonEC2).describeVpcs();
        doReturn(new CreateSecurityGroupResult().withGroupId(chaosSecurityGroupID)).when(amazonEC2)
                                                                                   .createSecurityGroup(new CreateSecurityGroupRequest()
                                                                                           .withGroupName(AWS_RDS_CHAOS_SECURITY_GROUP)
                                                                                           .withVpcId(defaultVpcId)
                                                                                           .withDescription(AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION));
        awsRDSPlatform.getChaosSecurityGroup();
    }

    @Test
    public void generateExperimentRoster () {
        Collection<Container> matchSet1 = new HashSet<>();
        Collection<Container> matchSet2 = new HashSet<>();
        DescribeDBInstancesResult describeDBInstancesResult = new DescribeDBInstancesResult();
        String availabilityZone1 = randomUUID().toString();
        String availabilityZone2;
        do {
            availabilityZone2 = randomUUID().toString();
        } while (availabilityZone1.equals(availabilityZone2));
        for (int i = 0; i < 25; i++) {
            String dBInstanceIdentifier = randomUUID().toString();
            String engine = randomUUID().toString();
            DBInstance dbInstance = new DBInstance().withDBInstanceIdentifier(dBInstanceIdentifier)
                                                    .withAvailabilityZone(availabilityZone1)
                                                    .withEngine(engine);
            describeDBInstancesResult.withDBInstances(dbInstance);
            matchSet1.add(AwsRDSInstanceContainer.builder()
                                                 .withAwsRDSPlatform(awsRDSPlatform)
                                                 .withAvailabilityZone(availabilityZone1)
                                                 .withDbInstanceIdentifier(dBInstanceIdentifier)
                                                 .withEngine(engine)
                                                 .build());
        }
        for (int i = 0; i < 25; i++) {
            String dBInstanceIdentifier = randomUUID().toString();
            String engine = randomUUID().toString();
            DBInstance dbInstance = new DBInstance().withDBInstanceIdentifier(dBInstanceIdentifier)
                                                    .withAvailabilityZone(availabilityZone2)
                                                    .withEngine(engine);
            describeDBInstancesResult.withDBInstances(dbInstance);
            matchSet2.add(AwsRDSInstanceContainer.builder()
                                                 .withAwsRDSPlatform(awsRDSPlatform)
                                                 .withAvailabilityZone(availabilityZone2)
                                                 .withDbInstanceIdentifier(dBInstanceIdentifier)
                                                 .withEngine(engine)
                                                 .build());
        }
        DescribeDBClustersResult describeDBClustersResult = new DescribeDBClustersResult();
        for (int i = 0; i < 15; i++) {
            String dbClusterIdentifier = randomUUID().toString();
            String engine = randomUUID().toString();
            DBCluster dbCluster = new DBCluster().withDBClusterIdentifier(dbClusterIdentifier).withEngine(engine);
            Container container = AwsRDSClusterContainer.builder()
                                                        .withAwsRDSPlatform(awsRDSPlatform)
                                                        .withDbClusterIdentifier(dbClusterIdentifier)
                                                        .withEngine(engine)
                                                        .build();
            matchSet1.add(container);
            matchSet2.add(container);
            describeDBClustersResult.withDBClusters(dbCluster);
        }
        doReturn(describeDBInstancesResult).when(amazonRDS).describeDBInstances(any(DescribeDBInstancesRequest.class));
        doReturn(describeDBClustersResult).when(amazonRDS).describeDBClusters(any(DescribeDBClustersRequest.class));
        assertThat(awsRDSPlatform.generateExperimentRoster(), anyOf(IsIterableContainingInAnyOrder.containsInAnyOrder(matchSet1
                .toArray(new Container[]{})), IsIterableContainingInAnyOrder.containsInAnyOrder(matchSet2.toArray(new Container[]{}))));
    }

    @Test
    public void createContainerFromDBInstance () {
        DBInstance dbInstance;
        AwsRDSInstanceContainer container;
        String engine = randomUUID().toString();
        String availabilityZone = randomUUID().toString();
        String dbInstanceIdentifier = randomUUID().toString();
        container = AwsRDSInstanceContainer.builder()
                                           .withAvailabilityZone(availabilityZone)
                                           .withDbInstanceIdentifier(dbInstanceIdentifier)
                                           .withEngine(engine)
                                           .build();
        dbInstance = new DBInstance().withEngine(engine)
                                     .withAvailabilityZone(availabilityZone)
                                     .withDBInstanceIdentifier(dbInstanceIdentifier);
        AwsRDSInstanceContainer actualContainer = awsRDSPlatform.createContainerFromDBInstance(dbInstance);
        assertEquals(container, actualContainer);
        verify(containerManager, times(1)).offer(any());
        verify(containerManager, times(1)).getMatchingContainer(AwsRDSInstanceContainer.class, dbInstanceIdentifier);
        reset(awsRDSPlatform, containerManager);
        assertSame(actualContainer, awsRDSPlatform.createContainerFromDBInstance(dbInstance));
        verify(containerManager, times(0)).offer(any());
        verify(containerManager, times(1)).getMatchingContainer(AwsRDSInstanceContainer.class, dbInstanceIdentifier);
    }

    @Test
    public void createContainerFromDBCluster () {
        DBCluster dbCluster;
        AwsRDSClusterContainer container;
        String engine = randomUUID().toString();
        String dbClusterIdentifier = randomUUID().toString();
        container = AwsRDSClusterContainer.builder()
                                          .withDbClusterIdentifier(dbClusterIdentifier)
                                          .withEngine(engine)
                                          .build();
        dbCluster = new DBCluster().withEngine(engine).withDBClusterIdentifier(dbClusterIdentifier);
        AwsRDSClusterContainer actualContainer = awsRDSPlatform.createContainerFromDBCluster(dbCluster);
        assertEquals(container, actualContainer);
        verify(containerManager, times(1)).offer(any());
        verify(containerManager, times(1)).getMatchingContainer(AwsRDSClusterContainer.class, dbClusterIdentifier);
        reset(awsRDSPlatform, containerManager);
        assertSame(actualContainer, awsRDSPlatform.createContainerFromDBCluster(dbCluster));
        verify(containerManager, times(0)).offer(any());
        verify(containerManager, times(1)).getMatchingContainer(AwsRDSClusterContainer.class, dbClusterIdentifier);
    }

    @Test
    public void createDBInstanceSnapshot () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        DBSnapshot dbSnapshot = mock(DBSnapshot.class);
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doReturn(dbSnapshot).when(amazonRDS)
                            .createDBSnapshot(new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                           .withDBSnapshotIdentifier(snapshotName));
        assertSame(dbSnapshot, awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier));
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceDBSnapshotAlreadyExistsException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new DBSnapshotAlreadyExistsException("Test")).when(amazonRDS)
                                                             .createDBSnapshot(new CreateDBSnapshotRequest()
                                                                                                            .withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                                            .withDBSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceSnapshotInvalidDBInstanceStateException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new InvalidDBInstanceStateException("Test")).when(amazonRDS)
                                                            .createDBSnapshot(new CreateDBSnapshotRequest()
                                                                                                           .withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                                           .withDBSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceSnapshotDBInstanceNotFoundException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new DBInstanceNotFoundException("Test")).when(amazonRDS).createDBSnapshot(new CreateDBSnapshotRequest()
                                                                                                       .withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                                       .withDBSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceSnapshotQuotaExceededException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new SnapshotQuotaExceededException("Test")).when(amazonRDS)
                                                           .createDBSnapshot(new CreateDBSnapshotRequest()
                                                                                                          .withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                                          .withDBSnapshotIdentifier(snapshotName));
        doNothing().when(awsRDSPlatform).cleanupOldSnapshots();
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test
    public void createDBInstanceSnapshotQuotaCleaned () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        DBSnapshot dbSnapshot = mock(DBSnapshot.class);
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new SnapshotQuotaExceededException("Test")).doReturn(dbSnapshot)
                                                           .when(amazonRDS)
                                                           .createDBSnapshot(new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                                          .withDBSnapshotIdentifier(snapshotName));
        doNothing().when(awsRDSPlatform).cleanupOldSnapshots();
        assertSame(dbSnapshot, awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier));
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceSnapshotRuntimeException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new RuntimeException("Test")).when(amazonRDS)
                                             .createDBSnapshot(new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                            .withDBSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void createDBInstanceSnapshotAmazonRDSRuntimeException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbInstanceIdentifier);
        doThrow(new AmazonRDSException("Test")).when(amazonRDS)
                                               .createDBSnapshot(new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                              .withDBSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
    }

    @Test
    public void snapshotDBCluster () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        DBClusterSnapshot dbClusterSnapshot = mock(DBClusterSnapshot.class);
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doReturn(dbClusterSnapshot).when(amazonRDS)
                                   .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest().withDBClusterIdentifier(dbClusterIdentifier)
                                                                                                .withDBClusterSnapshotIdentifier(snapshotName));
        assertSame(dbClusterSnapshot, awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier));
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterDBClusterSnapshotAlreadyExistsException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new DBClusterSnapshotAlreadyExistsException("Test")).when(amazonRDS)
                                                                    .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                            .withDBClusterIdentifier(dbClusterIdentifier)
                                                                            .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterInvalidDBClusterStateException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new InvalidDBClusterStateException("Test")).when(amazonRDS)
                                                           .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                   .withDBClusterIdentifier(dbClusterIdentifier)
                                                                   .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterDBClusterNotFoundException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new DBClusterNotFoundException("Test")).when(amazonRDS)
                                                       .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                                                                    .withDBClusterIdentifier(dbClusterIdentifier)
                                                                                                                    .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterSnapshotQuotaExceededException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new SnapshotQuotaExceededException("Test")).when(amazonRDS)
                                                           .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                   .withDBClusterIdentifier(dbClusterIdentifier)
                                                                   .withDBClusterSnapshotIdentifier(snapshotName));
        doNothing().when(awsRDSPlatform).cleanupOldSnapshots();
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test
    public void snapshotDBClusterSnapshotQuotaExceededCleaned () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        DBClusterSnapshot dbClusterSnapshot = Mockito.mock(DBClusterSnapshot.class);
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new SnapshotQuotaExceededException("Test")).doReturn(dbClusterSnapshot)
                                                           .when(amazonRDS)
                                                           .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                   .withDBClusterIdentifier(dbClusterIdentifier)
                                                                   .withDBClusterSnapshotIdentifier(snapshotName));
        doNothing().when(awsRDSPlatform).cleanupOldSnapshots();
        assertSame(dbClusterSnapshot, awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier));
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterInvalidDBClusterSnapshotStateException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new InvalidDBClusterSnapshotStateException("Test")).when(amazonRDS)
                                                                   .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest()
                                                                           .withDBClusterIdentifier(dbClusterIdentifier)
                                                                           .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterRuntimeException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new RuntimeException("Test")).when(amazonRDS)
                                             .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest().withDBClusterIdentifier(dbClusterIdentifier)
                                                                                                          .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test(expected = ChaosException.class)
    public void snapshotDBClusterAmazonRDSRuntimeException () {
        String snapshotName = UUID.randomUUID().toString();
        String dbClusterIdentifier = UUID.randomUUID().toString();
        doReturn(snapshotName).when(awsRDSPlatform).getDBSnapshotIdentifier(dbClusterIdentifier);
        doThrow(new AmazonRDSException("Test")).when(amazonRDS)
                                               .createDBClusterSnapshot(new CreateDBClusterSnapshotRequest().withDBClusterIdentifier(dbClusterIdentifier)
                                                                                                            .withDBClusterSnapshotIdentifier(snapshotName));
        awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
    }

    @Test
    public void getDBSnapshotIdentifier () {
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        Instant before = Instant.now();
        String actual = awsRDSPlatform.getDBSnapshotIdentifier(dbInstanceIdentifier);
        Instant after = Instant.now();
        Matcher m = CalendarUtils.datePattern.matcher(actual);
        assertTrue("Could not extract date-time from snapshot identifier", m.find());

        String dateSection = m.group(1);
        Instant i = AwsRDSUtils.getInstantFromNameSegment(dateSection);
        assertEquals(actual, String.format("ChaosSnapshot-%s-%s", dbInstanceIdentifier, dateSection));
        assertTrue("Snapshot given timestamp in the past", !before.isAfter(i));
        assertTrue("Snapshot given time in the future", !after.isBefore(i));
    }

    @Test
    public void cleanOldSnapshots1 () {
        doNothing().when(awsRDSPlatform).cleanupOldClusterSnapshots(anyInt());
        doNothing().when(awsRDSPlatform).cleanupOldInstanceSnapshots(anyInt());
        doCallRealMethod().when(awsRDSPlatform).cleanupOldSnapshots();
        awsRDSPlatform.cleanupOldSnapshots();
        verify(awsRDSPlatform, times(1)).cleanupOldInstanceSnapshots(anyInt());
        verify(awsRDSPlatform, times(1)).cleanupOldClusterSnapshots(anyInt());
    }

    @Test
    public void cleanOldSnapshots2 () {
        int olderThanMinutes = new Random().nextInt(1000) + 1;
        doNothing().when(awsRDSPlatform).cleanupOldClusterSnapshots(olderThanMinutes);
        doNothing().when(awsRDSPlatform).cleanupOldInstanceSnapshots(olderThanMinutes);
        doCallRealMethod().when(awsRDSPlatform).cleanupOldSnapshots(olderThanMinutes);
        awsRDSPlatform.cleanupOldSnapshots(olderThanMinutes);
        verify(awsRDSPlatform, times(1)).cleanupOldInstanceSnapshots(olderThanMinutes);
        verify(awsRDSPlatform, times(1)).cleanupOldClusterSnapshots(olderThanMinutes);
    }

    @Test
    public void cleanupOldInstanceSnapshots () {
        DBSnapshot dbSnapshot1 = new DBSnapshot().withDBSnapshotIdentifier("ChaosSnapshot-instance-1970-01-01T01-02-03Z");
        DBSnapshot dbSnapshot2 = new DBSnapshot().withDBSnapshotIdentifier("ChaosSnapshot-instance-2199-01-01T01-02-03Z");
        DBSnapshot dbSnapshot3 = new DBSnapshot().withDBSnapshotIdentifier("RegularSnapshot-instance-1970-01-01T01-02-03Z");
        DescribeDBSnapshotsResult describeDBSnapshotsResult = Mockito.mock(DescribeDBSnapshotsResult.class);
        doReturn(Arrays.asList(dbSnapshot1, dbSnapshot2, dbSnapshot3), Collections.singletonList(dbSnapshot2), Collections
                .singletonList(dbSnapshot3)).when(describeDBSnapshotsResult).getDBSnapshots();
        doReturn(describeDBSnapshotsResult).when(amazonRDS).describeDBSnapshots();
        doNothing().when(awsRDSPlatform).deleteInstanceSnapshot(any());
        awsRDSPlatform.cleanupOldInstanceSnapshots(60);
        verify(awsRDSPlatform, times(1)).deleteInstanceSnapshot(dbSnapshot1);
        // Test that it doesn't call on snapshot2 with individual return lists.
        reset(awsRDSPlatform);
        awsRDSPlatform.cleanupOldInstanceSnapshots(60);
        verify(awsRDSPlatform, never()).deleteInstanceSnapshot(any());
        // Test that it doesn't call on snapshot3 with individual return lists.
        reset(awsRDSPlatform);
        awsRDSPlatform.cleanupOldInstanceSnapshots(60);
        verify(awsRDSPlatform, never()).deleteInstanceSnapshot(any());
    }

    @Test
    public void cleanupOldClusterSnapshots () {
        DBClusterSnapshot dbClusterSnapshot1 = new DBClusterSnapshot().withDBClusterSnapshotIdentifier("ChaosSnapshot-instance-1970-01-01T01-02-03Z");
        DBClusterSnapshot dbClusterSnapshot2 = new DBClusterSnapshot().withDBClusterSnapshotIdentifier("ChaosSnapshot-instance-2199-01-01T01-02-03Z");
        DBClusterSnapshot dbClusterSnapshot3 = new DBClusterSnapshot().withDBClusterSnapshotIdentifier("RegularSnapshot-instance-1970-01-01T01-02-03Z");
        DescribeDBClusterSnapshotsResult describeDBClusterSnapshotsResult = Mockito.mock(DescribeDBClusterSnapshotsResult.class);
        doReturn(Arrays.asList(dbClusterSnapshot1, dbClusterSnapshot2, dbClusterSnapshot3), Collections.singletonList(dbClusterSnapshot2), Collections
                .singletonList(dbClusterSnapshot3)).when(describeDBClusterSnapshotsResult).getDBClusterSnapshots();
        doReturn(describeDBClusterSnapshotsResult).when(amazonRDS).describeDBClusterSnapshots();
        doNothing().when(awsRDSPlatform).deleteClusterSnapshot(any());
        awsRDSPlatform.cleanupOldClusterSnapshots(60);
        verify(awsRDSPlatform, times(1)).deleteClusterSnapshot(dbClusterSnapshot1);
        reset(awsRDSPlatform);
        awsRDSPlatform.cleanupOldClusterSnapshots(60);
        verify(awsRDSPlatform, never()).deleteClusterSnapshot(any());
        reset(awsRDSPlatform);
        awsRDSPlatform.cleanupOldClusterSnapshots(60);
        verify(awsRDSPlatform, never()).deleteClusterSnapshot(any());
        reset(awsRDSPlatform);
    }

    @Test
    public void snapshotIsOlderThan () {
        int minutes = new Random().nextInt(23 * 60) + 30;
        Instant baseTime = Instant.now().minus(Duration.ofMinutes(minutes));
        Instant olderTime = baseTime.minus(Duration.ofMinutes(20));
        Instant newerTime = baseTime.plus(Duration.ofMinutes(20));
        String olderSnapshotName = String.format("ChaosSnapshot-%s-%s", randomUUID(), olderTime)
                                         .replaceAll(":", "-")
                                         .replaceAll(",", "-")
                                         .replaceAll("\\.", "-")
                                         .replaceAll("--", "-");
        String newerSnapshotName = String.format("ChaosSnapshot-%s-%s", randomUUID(), newerTime)
                                         .replaceAll(":", "-")
                                         .replaceAll(",", "-")
                                         .replaceAll("\\.", "-")
                                         .replaceAll("--", "-");
        String otherSnapshotName = randomUUID().toString();
        assertTrue("This snapshot should be treated as old and deletable", awsRDSPlatform.snapshotIsOlderThan(olderSnapshotName, minutes));
        assertTrue("This snapshot should be treated as old and deletable", awsRDSPlatform.snapshotIsOlderThan(olderSnapshotName
                .toLowerCase(), minutes));
        assertFalse("This snapshot should be treated as new and not deletable", awsRDSPlatform.snapshotIsOlderThan(newerSnapshotName, minutes));
        assertFalse("This snapshot should be treated as new and not deletable", awsRDSPlatform.snapshotIsOlderThan(newerSnapshotName
                .toLowerCase(), minutes));
        assertFalse("This snapshot doesn't belong to Chaos and should not be deletable", awsRDSPlatform.snapshotIsOlderThan(otherSnapshotName, minutes));
        assertFalse("This snapshot doesn't belong to Chaos and should not be deletable", awsRDSPlatform.snapshotIsOlderThan(otherSnapshotName
                .toLowerCase(), minutes));
    }

    @Test
    public void deleteInstanceSnapshot () {
        String dBSnapshotIdentifier = randomUUID().toString();
        DBSnapshot dbSnapshot = new DBSnapshot().withDBSnapshotIdentifier(dBSnapshotIdentifier);
        doReturn(null).when(amazonRDS).deleteDBSnapshot(any());
        awsRDSPlatform.deleteInstanceSnapshot(dbSnapshot);
        verify(amazonRDS, times(1)).deleteDBSnapshot(new DeleteDBSnapshotRequest().withDBSnapshotIdentifier(dBSnapshotIdentifier));
    }

    @Test
    public void deleteClusterSnapshot () {
        String dBClusterSnapshotIdentifier = randomUUID().toString();
        DBClusterSnapshot dbClusterSnapshot = new DBClusterSnapshot().withDBClusterSnapshotIdentifier(dBClusterSnapshotIdentifier);
        doReturn(null).when(amazonRDS).deleteDBClusterSnapshot(any());
        awsRDSPlatform.deleteClusterSnapshot(dbClusterSnapshot);
        verify(amazonRDS, times(1)).deleteDBClusterSnapshot(new DeleteDBClusterSnapshotRequest().withDBClusterSnapshotIdentifier(dBClusterSnapshotIdentifier));
    }

    @Test
    public void isInstanceSnapshotRunning () {
        String dbInstanceIdentifier = randomUUID().toString();
        DBInstance backingUpInstance = new DBInstance().withDBInstanceStatus(AWS_RDS_BACKING_UP)
                                                       .withDBInstanceIdentifier(dbInstanceIdentifier);
        DBInstance normalInstance = new DBInstance().withDBInstanceStatus(AWS_RDS_AVAILABLE);
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier);
        DescribeDBInstancesResult describeDBInstancesResult1 = new DescribeDBInstancesResult().withDBInstances(backingUpInstance);
        DescribeDBInstancesResult describeDBInstancesResult2 = new DescribeDBInstancesResult().withDBInstances(backingUpInstance, normalInstance);
        DescribeDBInstancesResult describeDBInstancesResult3 = new DescribeDBInstancesResult().withDBInstances(normalInstance
                .withDBInstanceIdentifier(dbInstanceIdentifier));
        doReturn(describeDBInstancesResult1, describeDBInstancesResult2, describeDBInstancesResult3).when(amazonRDS)
                                                                                                    .describeDBInstances(describeDBInstancesRequest);
        assertTrue("Exactly one instance backing up should return true", awsRDSPlatform.isInstanceSnapshotRunning(dbInstanceIdentifier));
        assertTrue("Any instance backing up should return true", awsRDSPlatform.isInstanceSnapshotRunning(dbInstanceIdentifier));
        assertFalse("No instances backing up should return false", awsRDSPlatform.isInstanceSnapshotRunning(dbInstanceIdentifier));
    }

    @Test
    public void isClusterSnapshotRunning () {
        String dbClusterIdentifier = randomUUID().toString();
        DBCluster backingUpCluster = new DBCluster().withStatus(AWS_RDS_BACKING_UP)
                                                    .withDBClusterIdentifier(dbClusterIdentifier);
        DBCluster normalCluster = new DBCluster().withStatus(AWS_RDS_AVAILABLE);
        DescribeDBClustersResult describeDBClustersResult1 = new DescribeDBClustersResult().withDBClusters(backingUpCluster);
        DescribeDBClustersResult describeDBClustersResult2 = new DescribeDBClustersResult().withDBClusters(backingUpCluster, normalCluster);
        DescribeDBClustersResult describeDBClustersResult3 = new DescribeDBClustersResult().withDBClusters(normalCluster
                .withDBClusterIdentifier(dbClusterIdentifier));
        DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest().withDBClusterIdentifier(dbClusterIdentifier);
        doReturn(describeDBClustersResult1, describeDBClustersResult2, describeDBClustersResult3).when(amazonRDS)
                                                                                                 .describeDBClusters(describeDBClustersRequest);
        assertTrue("Exactly one instance backing up should return true", awsRDSPlatform.isClusterSnapshotRunning(dbClusterIdentifier));
        assertTrue("Any instance backing up should return true", awsRDSPlatform.isClusterSnapshotRunning(dbClusterIdentifier));
        assertFalse("No instances backing up should return false", awsRDSPlatform.isClusterSnapshotRunning(dbClusterIdentifier));
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private AmazonRDS amazonRDS;
        @Autowired
        private AmazonEC2 amazonEC2;

        @Bean
        AwsRDSPlatform awsRDSPlatform () {
            return Mockito.spy(new AwsRDSPlatform(amazonRDS, amazonEC2));
        }
    }
}