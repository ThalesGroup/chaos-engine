package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_AVAILABLE;
import static com.gemalto.chaos.container.enums.ContainerHealth.*;

@ConditionalOnBean(AmazonRDS.class)
@Component
public class AwsRDSPlatform extends Platform {
    @Autowired
    private AmazonRDS amazonRDS;

    @Override
    public ApiStatus getApiStatus () {
        try {
            amazonRDS.describeDBInstances();
            amazonRDS.describeDBClusters();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            log.error("API for AWS RDS failed to resolve.", e);
            return ApiStatus.ERROR;
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.IAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        Supplier<Stream<DBInstanceStatusInfo>> dbInstanceStatusInfoStreamSupplier = () -> amazonRDS.describeDBInstances()
                                                                                                   .getDBInstances()
                                                                                                   .stream()
                                                                                                   .map(DBInstance::getStatusInfos)
                                                                                                   .flatMap(Collection::stream);
        if (!dbInstanceStatusInfoStreamSupplier.get().allMatch(DBInstanceStatusInfo::getNormal)) {
            if (dbInstanceStatusInfoStreamSupplier.get().anyMatch(DBInstanceStatusInfo::getNormal)) {
                return PlatformHealth.DEGRADED;
            }
            return PlatformHealth.FAILED;
        }
        return PlatformHealth.OK;
    }

    @Override
    protected List<Container> generateRoster () {
        Collection<DBCluster> dbClusters = amazonRDS.describeDBClusters().getDBClusters();
        Collection<DBInstance> dbInstances = amazonRDS.describeDBInstances().getDBInstances();
        log.debug("Clusters: {}", dbClusters);
        log.debug("Instances: {}", dbInstances);
        Collection<Container> dbInstanceContainers = dbInstances.stream()
                                                                .filter(dbInstance -> dbInstance.getDBClusterIdentifier()
                                                                                                .isEmpty())
                                                                .map(this::createContainerFromDBInstance)
                                                                .collect(Collectors.toSet());
        Collection<Container> dbClusterContainers = dbClusters.stream()
                                                              .map(this::createContainerFromDBCluster)
                                                              .collect(Collectors.toSet());
        return Stream.of(dbClusterContainers, dbInstanceContainers)
                     .flatMap(Collection::stream)
                     .collect(Collectors.toList());
    }

    private Container createContainerFromDBInstance (DBInstance dbInstance) {
        return AwsRDSInstanceContainer.builder()
                                      .withAwsRDSPlatform(this)
                                      .withDbInstanceIdentifier(dbInstance.getDBInstanceIdentifier())
                                      .withEngine(dbInstance.getEngine())
                                      .build();
    }

    private Container createContainerFromDBCluster (DBCluster dbCluster) {
        return AwsRDSClusterContainer.builder()
                                     .withAwsRDSPlatform(this)
                                     .withDbClusterIdentifier(dbCluster.getDBClusterIdentifier())
                                     .withEngine(dbCluster.getEngine())
                                     .build();
    }

    ContainerHealth getDBInstanceHealth (AwsRDSInstanceContainer awsRDSInstanceContainer) {
        String instanceId = awsRDSInstanceContainer.getDbInstanceIdentifier();
        return getDBInstanceHealth(instanceId);
    }

    private ContainerHealth getDBInstanceHealth (String instanceId) {
        DBInstance dbInstance;
        try {
            dbInstance = amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceId))
                                  .getDBInstances()
                                  .get(0);
        } catch (IndexOutOfBoundsException e) {
            return DOES_NOT_EXIST;
        }
        return dbInstance.getStatusInfos()
                         .stream()
                         .anyMatch(dbInstanceStatusInfo -> dbInstanceStatusInfo.getNormal()
                                                                               .equals(false)) ? UNDER_ATTACK : NORMAL;
    }

    ContainerHealth getDBClusterHealth (AwsRDSClusterContainer awsRDSClusterContainer) {
        String clusterInstanceId = awsRDSClusterContainer.getDbClusterIdentifier();
        return getContainerHealth(clusterInstanceId);
    }

    private ContainerHealth getContainerHealth (String clusterInstanceId) {
        DBCluster dbCluster;
        try {
            dbCluster = amazonRDS.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(clusterInstanceId))
                                 .getDBClusters()
                                 .get(0);
        } catch (IndexOutOfBoundsException e) {
            return DOES_NOT_EXIST;
        }
        if (!dbCluster.getStatus().equals(AWS_RDS_AVAILABLE)) return UNDER_ATTACK;
        if (dbCluster.getDBClusterMembers()
                     .stream()
                     .map(DBClusterMember::getDBInstanceIdentifier)
                     .map(this::getDBInstanceHealth)
                     .allMatch(containerHealth -> containerHealth.equals(ContainerHealth.NORMAL))) return NORMAL;
        return UNDER_ATTACK;
    }

    @PostConstruct
    private void postConstruct () {
        log.info("Created AmazonRDS Platform");
    }
}
