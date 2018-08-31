package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AwsRDSPlatformTest {
    @MockBean
    private AmazonRDS amazonRDS;
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
        DBInstance normalDbInstance = new DBInstance().withStatusInfos(new DBInstanceStatusInfo().withNormal(true));
        DBInstance failedDbInstance = new DBInstance().withStatusInfos(new DBInstanceStatusInfo().withNormal(false));
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
        DBInstance dbInstance1 = new DBInstance().withDBInstanceIdentifier(dbInstance1Identifier).withMultiAZ(false);
        DBInstance dbInstance2 = new DBInstance().withDBInstanceIdentifier(dbInstance2Identifier).withMultiAZ(false);
        DBInstance clusterInstance = new DBInstance().withDBInstanceIdentifier(dbClusterInstanceIdentifier)
                                                     .withMultiAZ(true);
        DBCluster dbCluster1 = new DBCluster().withDBClusterIdentifier(dbCluster1Identifier);
        DBCluster dbCluster2 = new DBCluster().withDBClusterIdentifier(dbCluster2Identifier);
        doReturn(new DescribeDBInstancesResult().withDBInstances(dbInstance1, dbInstance2, clusterInstance)).when(amazonRDS)
                                                                                                            .describeDBInstances();
        doReturn(new DescribeDBClustersResult().withDBClusters(dbCluster1, dbCluster2)).when(amazonRDS)
                                                                                       .describeDBClusters();
//        fail("Implement this!"); // TODO : Finish
    }

    @Test
    public void getDBInstanceHealth () {
        String instanceId = UUID.randomUUID().toString();
        AwsRDSInstanceContainer awsRDSInstanceContainer = mock(AwsRDSInstanceContainer.class);
        doReturn(instanceId).when(awsRDSInstanceContainer).getDbInstanceIdentifier();
        doReturn(new DescribeDBInstancesResult().withDBInstances()).when(amazonRDS)
                                                                   .describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsRDSPlatform.getDBInstanceHealth(awsRDSInstanceContainer));
    }

    @Test
    public void getDBClusterHealth () {
        String clusterId = UUID.randomUUID().toString();
        String memberId1 = UUID.randomUUID().toString();
        String memberId2 = UUID.randomUUID().toString();
        AwsRDSClusterContainer awsRDSClusterContainer = mock(AwsRDSClusterContainer.class);
        doReturn(clusterId).when(awsRDSClusterContainer).getDbClusterIdentifier();
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
        doReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance().withStatusInfos(new DBInstanceStatusInfo()
                .withNormal(false)))).when(amazonRDS).describeDBInstances(any(DescribeDBInstancesRequest.class));
        assertEquals(ContainerHealth.UNDER_ATTACK, awsRDSPlatform.getDBClusterHealth(awsRDSClusterContainer));
    }

    @Test
    public void createContainerFromDBInstance () {
    }

    @Test
    public void createContainerFromDBCluster () {
    }

    @Configuration
    static class TestConfig {
        @Bean
        AwsRDSPlatform awsRDSPlatform () {
            return new AwsRDSPlatform();
        }
    }
}