package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_CHAOS_SECURITY_GROUP;
import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AwsRDSPlatformTest {
    @MockBean
    private AmazonRDS amazonRDS;
    @MockBean
    private AmazonEC2 amazonEC2;
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
        when(amazonRDS.describeDBInstances()).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.OK, awsRDSPlatform.getPlatformHealth());
        // Case 2: Many normal instances
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(normalDbInstance.clone(), normalDbInstance
                .clone(), normalDbInstance.clone());
        when(amazonRDS.describeDBInstances()).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.OK, awsRDSPlatform.getPlatformHealth());
        // Case 3: Mix of Normal and Failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(normalDbInstance.clone(), normalDbInstance
                .clone(), failedDbInstance.clone(), failedDbInstance.clone());
        when(amazonRDS.describeDBInstances()).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.DEGRADED, awsRDSPlatform.getPlatformHealth());
        // Case 4: One failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(failedDbInstance.clone());
        when(amazonRDS.describeDBInstances()).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.FAILED, awsRDSPlatform.getPlatformHealth());
        // Case 5: Many failed
        describeDBInstancesResult = new DescribeDBInstancesResult().withDBInstances(failedDbInstance.clone(), failedDbInstance
                .clone(), failedDbInstance.clone());
        when(amazonRDS.describeDBInstances()).thenReturn(describeDBInstancesResult);
        assertEquals(PlatformHealth.FAILED, awsRDSPlatform.getPlatformHealth());
    }

    @Test
    public void generateRoster () {
        String dbInstance1Identifier = UUID.randomUUID().toString();
        String dbInstance2Identifier = UUID.randomUUID().toString();
        String dbCluster1Identifier = UUID.randomUUID().toString();
        String dbCluster2Identifier = UUID.randomUUID().toString();
        String dbClusterInstanceIdentifier = UUID.randomUUID().toString();
        String dbInstance1ReadReplicaIdentifier = UUID.randomUUID().toString();
        DBInstance dbInstance1 = new DBInstance().withDBInstanceIdentifier(dbInstance1Identifier)
                                                 .withMultiAZ(true)
                                                 .withDBClusterIdentifier(null)
                                                 .withReadReplicaDBInstanceIdentifiers(dbInstance1ReadReplicaIdentifier);
        DBInstance replicaInstance = new DBInstance().withDBInstanceIdentifier(dbInstance1ReadReplicaIdentifier)
                                                     .withReadReplicaSourceDBInstanceIdentifier(dbInstance1Identifier);
        DBInstance dbInstance2 = new DBInstance().withDBInstanceIdentifier(dbInstance2Identifier)
                                                 .withMultiAZ(false)
                                                 .withDBClusterIdentifier(null);
        DBInstance clusterInstance = new DBInstance().withDBInstanceIdentifier(dbClusterInstanceIdentifier)
                                                     .withMultiAZ(true)
                                                     .withDBClusterIdentifier(dbCluster1Identifier);
        DBCluster dbCluster1 = new DBCluster().withDBClusterIdentifier(dbCluster1Identifier);
        DBCluster dbCluster2 = new DBCluster().withDBClusterIdentifier(dbCluster2Identifier);
        doReturn(new DescribeDBInstancesResult().withDBInstances(dbInstance1, dbInstance2, clusterInstance, replicaInstance))
                .when(amazonRDS)
                .describeDBInstances(any(DescribeDBInstancesRequest.class));
        doReturn(new DescribeDBClustersResult().withDBClusters(dbCluster1, dbCluster2)).when(amazonRDS)
                                                                                       .describeDBClusters(any(DescribeDBClustersRequest.class));
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
                                                                                 .withReadReplicas(Collections.singleton(dbInstance1ReadReplicaIdentifier))
                                                                                 .build(), AwsRDSInstanceContainer.builder()
                                                                                                                  .withAwsRDSPlatform(awsRDSPlatform)
                                                                                                                  .withDbInstanceIdentifier(dbInstance2Identifier)
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
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));
        doReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceStatus(AwsRDSConstants.AWS_RDS_AVAILABLE)))
                .when(amazonRDS)
                .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));




    }

    @Test
    public void getDBClusterHealth () {
        String memberId1 = UUID.randomUUID().toString();
        String memberId2 = UUID.randomUUID().toString();
        AwsRDSClusterContainer awsRDSClusterContainer = mock(AwsRDSClusterContainer.class);
        doReturn(new DescribeDBClustersResult().withDBClusters()).when(amazonRDS)
                                                                 .describeDBClusters(any(DescribeDBClustersRequest.class));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
        doReturn(new DescribeDBClustersResult().withDBClusters(new DBCluster().withStatus(AwsRDSConstants.AWS_RDS_FAILING_OVER)))
                .when(amazonRDS)
                .describeDBClusters(any(DescribeDBClustersRequest.class));
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
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
    public void createContainerFromDBInstance () {
    }

    @Test
    public void createContainerFromDBCluster () {
    }

    @Test
    public void failoverCluster () {
        String clusterIdentifier = UUID.randomUUID().toString();
        awsRDSPlatform.failoverCluster(clusterIdentifier);
        verify(amazonRDS, times(1)).failoverDBCluster(new FailoverDBClusterRequest().withDBClusterIdentifier(clusterIdentifier));
    }

    @Test
    public void restartInstance () {
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();
        awsRDSPlatform.restartInstance(instanceId1);
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId1)
                                                                                  .withForceFailover(false));
        Mockito.reset(amazonRDS);
        awsRDSPlatform.restartInstance(instanceId1, instanceId2);
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId1)
                                                                                  .withForceFailover(true));
        verify(amazonRDS, times(1)).rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(instanceId2)
                                                                                  .withForceFailover(true));
    }

    @Test
    public void getClusterInstances () {
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();
        String clusterId = UUID.randomUUID().toString();
        DescribeDBClustersResult dbClustersResult = new DescribeDBClustersResult().withDBClusters(new DBCluster().withDBClusterMembers(new DBClusterMember()
                .withDBInstanceIdentifier(instanceId1), new DBClusterMember().withDBInstanceIdentifier(instanceId2)));
        doReturn(dbClustersResult).when(amazonRDS)
                                  .describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(clusterId));
        assertThat(awsRDSPlatform.getClusterInstances(clusterId), IsIterableContainingInAnyOrder.containsInAnyOrder(instanceId1, instanceId2));
    }

    @Test
    public void getInstanceStatus () {
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();
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
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.getInstanceStatus(instanceId1, instanceId2));
        reset(amazonRDS);
        doReturn(abnormalInstance1).when(amazonRDS)
                                   .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId1));
        doReturn(normalInstance2).when(amazonRDS)
                                 .describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId2));
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.getInstanceStatus(instanceId1, instanceId2));
    }

    @Test
    public void getVpcSecurityGroupIds () {
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        Collection<String> expectedVpcIds = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            expectedVpcIds.add(UUID.randomUUID().toString());
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
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        String vpcSecurityGroupId = UUID.randomUUID().toString();
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupId);
        verify(amazonRDS, times(1)).modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(Collections
                .singleton(vpcSecurityGroupId)));
    }

    @Test
    public void setVpcSecurityGroupIds1 () {
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            vpcSecurityGroupIds.add(UUID.randomUUID().toString());
        }
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds);
        verify(amazonRDS, times(1)).modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(vpcSecurityGroupIds));
    }

    @Test
    public void checkVpcSecurityGroupIds () {
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        vpcSecurityGroupIds.add(UUID.randomUUID().toString());
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
        String dbInstanceIdentifier = UUID.randomUUID().toString();
        Collection<String> vpcSecurityGroupIds = new HashSet<>();
        List<String> reverseOrderSet = new ArrayList<>();
        for (int x = 0; x < 100; x++) {
            String randomString = UUID.randomUUID().toString();
            vpcSecurityGroupIds.add(randomString);
            reverseOrderSet.add(randomString);
        }
        Collections.reverse(reverseOrderSet);
        Collection<String> biggerSet = new HashSet<>(vpcSecurityGroupIds);
        biggerSet.add(UUID.randomUUID().toString());
        Collection<String> smallerSet = new HashSet<>(vpcSecurityGroupIds);
        smallerSet.remove(vpcSecurityGroupIds.iterator().next());
        when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier)))
                .thenReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withVpcSecurityGroups(vpcSecurityGroupIds
                        .stream()
                        .map(vpcIds -> new VpcSecurityGroupMembership().withVpcSecurityGroupId(vpcIds))
                        .collect(Collectors.toSet()))));
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds
                .iterator()
                .next()));
        // Two equal sets
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, vpcSecurityGroupIds));
        // Second set is in different order
        assertEquals(ContainerHealth.NORMAL, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, reverseOrderSet));
        // Second set has one more item
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, biggerSet));
        // Second set has one less item
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, smallerSet));
    }

    @Test
    public void getChaosSecurityGroup () {
        awsRDSPlatform = Mockito.spy(new AwsRDSPlatform(amazonRDS, amazonEC2));
        String defaultVpcId = UUID.randomUUID().toString();
        String customVpcId = UUID.randomUUID().toString();
        String defaultSecurityGroupID = UUID.randomUUID().toString();
        String customSecurityGroupID = UUID.randomUUID().toString();
        String chaosSecurityGroupID = UUID.randomUUID().toString();
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
        awsRDSPlatform = Mockito.spy(new AwsRDSPlatform(amazonRDS, amazonEC2));
        String defaultVpcId = UUID.randomUUID().toString();
        String customVpcId = UUID.randomUUID().toString();
        String defaultSecurityGroupID = UUID.randomUUID().toString();
        String customSecurityGroupID = UUID.randomUUID().toString();
        String chaosSecurityGroupID = UUID.randomUUID().toString();
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
        awsRDSPlatform = Mockito.spy(new AwsRDSPlatform(amazonRDS, amazonEC2));
        String defaultVpcId = UUID.randomUUID().toString();
        String customVpcId = UUID.randomUUID().toString();
        String defaultSecurityGroupID = UUID.randomUUID().toString();
        String customSecurityGroupID = UUID.randomUUID().toString();
        String chaosSecurityGroupID = UUID.randomUUID().toString();
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

    @Configuration
    static class TestConfig {
        @Autowired
        private AmazonRDS amazonRDS;
        @Autowired
        private AmazonEC2 amazonEC2;

        @Bean
        AwsRDSPlatform awsRDSPlatform () {
            return new AwsRDSPlatform(amazonRDS, amazonEC2);
        }
    }
}