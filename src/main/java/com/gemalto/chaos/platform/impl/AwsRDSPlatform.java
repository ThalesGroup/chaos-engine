package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_AVAILABLE;
import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_CHAOS_SECURITY_GROUP;
import static com.gemalto.chaos.container.enums.ContainerHealth.*;

@ConditionalOnProperty("aws.rds")
@Component
public class AwsRDSPlatform extends Platform {
    @Autowired
    private AmazonRDS amazonRDS;
    @Autowired
    private AmazonEC2 amazonEC2;
    private String defaultVpcId;
    private String chaosSecurityGroup;

    @Autowired
    public AwsRDSPlatform () {
    }

    AwsRDSPlatform (AmazonRDS amazonRDS, AmazonEC2 amazonEC2) {
        this.amazonRDS = amazonRDS;
        this.amazonEC2 = amazonEC2;
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
        Supplier<Stream<String>> dbInstanceStatusSupplier = () -> amazonRDS.describeDBInstances()
                                                                           .getDBInstances()
                                                                           .stream()
                                                                           .map(DBInstance::getDBInstanceStatus);
        if (!dbInstanceStatusSupplier.get().allMatch(s -> s.equals(AwsRDSConstants.AWS_RDS_AVAILABLE))) {
            if (dbInstanceStatusSupplier.get().anyMatch(s -> s.equals(AwsRDSConstants.AWS_RDS_AVAILABLE))) {
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
        return dbInstance.getDBInstanceStatus().equals(AwsRDSConstants.AWS_RDS_AVAILABLE) ? NORMAL : UNDER_ATTACK;
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
            restartInstance(dbInstanceIdentifier, dbInstanceIdentifiers.length > 1);
        }
    }

    private void restartInstance (String dbInstanceIdentifier, Boolean failover) {
        amazonRDS.rebootDBInstance(new RebootDBInstanceRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                .withForceFailover(failover));
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
        Supplier<Stream<String>> dbInstanceStatusSupplier = () -> dbInstances.stream()
                                                                             .map(DBInstance::getDBInstanceStatus);
        if (dbInstanceStatusSupplier.get().count() == 0) {
            return ContainerHealth.DOES_NOT_EXIST;
        } else if (dbInstanceStatusSupplier.get().noneMatch(s -> s.equals(AwsRDSConstants.AWS_RDS_AVAILABLE))) {
            return ContainerHealth.UNDER_ATTACK;
        }
        return ContainerHealth.NORMAL;
    }

    public void setVpcSecurityGroupIds (String dbInstanceIdentifier, String vpcSecurityGroupId) {
        setVpcSecurityGroupIds(dbInstanceIdentifier, Collections.singleton(vpcSecurityGroupId));
    }

    public void setVpcSecurityGroupIds (String dbInstanceIdentifier, Collection<String> vpcSecurityGroupIds) {
        amazonRDS.modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(vpcSecurityGroupIds));
    }

    public ContainerHealth checkVpcSecurityGroupIds (String dbInstanceIdentifier, String vpcSecurityGroupId) {
        return checkVpcSecurityGroupIds(dbInstanceIdentifier, Collections.singleton(vpcSecurityGroupId));
    }

    public ContainerHealth checkVpcSecurityGroupIds (String dbInstanceIdentifier, Collection<String> vpcSecurityGroupIds) {
        Collection<String> actualVpcSecurityGroupIds = getVpcSecurityGroupIds(dbInstanceIdentifier);
        return actualVpcSecurityGroupIds.containsAll(vpcSecurityGroupIds) && vpcSecurityGroupIds.containsAll(actualVpcSecurityGroupIds) ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK;
    }

    public Collection<String> getVpcSecurityGroupIds (String dbInstanceIdentifier) {
        return amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                        .getDBInstances()
                        .stream()
                        .map(DBInstance::getVpcSecurityGroups)
                        .flatMap(Collection::stream)
                        .map(VpcSecurityGroupMembership::getVpcSecurityGroupId)
                        .collect(Collectors.toSet());
    }

    public String getChaosSecurityGroup () {
        if (chaosSecurityGroup == null) initChaosSecurityGroup();
        return chaosSecurityGroup;
    }

    void initChaosSecurityGroup () {
        amazonEC2.describeSecurityGroups()
                 .getSecurityGroups()
                 .stream()
                 .filter(securityGroup -> securityGroup.getGroupName().equals(AWS_RDS_CHAOS_SECURITY_GROUP))
                 .findFirst()
                 .ifPresent(securityGroup -> chaosSecurityGroup = securityGroup.getGroupId());
        if (chaosSecurityGroup == null) {
            chaosSecurityGroup = createChaosSecurityGroup();
        }
    }

    private String createChaosSecurityGroup () {
        amazonEC2.describeVpcs()
                 .getVpcs()
                 .stream()
                 .filter(Vpc::isDefault)
                 .findFirst()
                 .ifPresent(vpc -> defaultVpcId = vpc.getVpcId());
        if (defaultVpcId == null) {
            throw new ChaosException("No Default VPC Found");
        }
        return amazonEC2.createSecurityGroup(new CreateSecurityGroupRequest().withVpcId(defaultVpcId)
                                                                             .withDescription(AwsRDSConstants.AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION)
                                                                             .withGroupName(AWS_RDS_CHAOS_SECURITY_GROUP))
                        .getGroupId();
    }
}
