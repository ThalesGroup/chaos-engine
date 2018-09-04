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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_AVAILABLE;
import static com.gemalto.chaos.container.enums.ContainerHealth.*;

@ConditionalOnBean(AmazonRDS.class)
@Component
public class AwsRDSPlatform extends Platform {
    private AmazonRDS amazonRDS;

    public AwsRDSPlatform (AmazonRDS amazonRDS) {
        this.amazonRDS = amazonRDS;
    }

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
        Collection<Container> dbInstanceContainers = dbInstances.stream()
                                                                .filter(dbInstance -> dbInstance.getDBClusterIdentifier() == null)
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

    public void failoverCluster (String dbClusterIdentifier) {
        amazonRDS.failoverDBCluster(new FailoverDBClusterRequest().withDBClusterIdentifier(dbClusterIdentifier));
    }

    public void restartInstance (String... dbInstanceIdentifiers) {
        for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
            restartInstance(dbInstanceIdentifier);
        }
    }

    void restartInstance (String dbInstanceIdentifier) {
        amazonRDS.rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(dbInstanceIdentifier));
    }

    public Set<String> getClusterInstances (String dbClusterIdentifier) {
        return amazonRDS.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(dbClusterIdentifier))
                        .getDBClusters()
                        .stream()
                        .map(DBCluster::getDBClusterMembers)
                        .flatMap(Collection::stream)
                        .map(DBClusterMember::getDBInstanceIdentifier)
                        .collect(Collectors.toSet());
    }

    public ContainerHealth getInstanceStatus (String... dbInstanceIdentifiers) {
        Collection<ContainerHealth> containerHealthCollection = new HashSet<>();
        for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
            containerHealthCollection.add(getInstanceStatus(dbInstanceIdentifier));
        }
        if (containerHealthCollection.stream()
                                     .anyMatch(containerHealth -> containerHealth.equals(ContainerHealth.DOES_NOT_EXIST))) {
            return ContainerHealth.DOES_NOT_EXIST;
        } else if (containerHealthCollection.stream()
                                            .anyMatch(containerHealth -> containerHealth.equals(ContainerHealth.UNDER_ATTACK))) {
            return ContainerHealth.UNDER_ATTACK;
        }
        return ContainerHealth.NORMAL;
    }

    private ContainerHealth getInstanceStatus (String dbInstanceIdentifier) {
        List<DBInstance> dbInstances = amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                                                .getDBInstances();
        Supplier<Stream<DBInstanceStatusInfo>> dbInstanceStatusInfo = () -> dbInstances.stream()
                                                                                       .map(DBInstance::getStatusInfos)
                                                                                       .flatMap(Collection::stream);
        if (dbInstanceStatusInfo.get().count() == 0) {
            return ContainerHealth.DOES_NOT_EXIST;
        } else if (dbInstanceStatusInfo.get().anyMatch(dbInstanceStatusInfo1 -> !dbInstanceStatusInfo1.getNormal())) {
            return ContainerHealth.UNDER_ATTACK;
        }
        return ContainerHealth.NORMAL;
    }
}
