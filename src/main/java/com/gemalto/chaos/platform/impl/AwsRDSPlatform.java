package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsRDSClusterContainer;
import com.gemalto.chaos.container.impl.AwsRDSInstanceContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.util.AwsRDSUtils;
import com.gemalto.chaos.util.CalendarUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsConstants.NO_AZ_INFORMATION;
import static com.gemalto.chaos.constants.AwsRDSConstants.*;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_PLATFORM_KEY;
import static com.gemalto.chaos.container.enums.ContainerHealth.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static net.logstash.logback.argument.StructuredArguments.*;

@ConditionalOnProperty("aws.rds")
@ConfigurationProperties("aws.rds")
@Component
public class AwsRDSPlatform extends Platform {
    @Autowired
    private AmazonRDS amazonRDS;
    @Autowired
    private AmazonEC2 amazonEC2;
    @Autowired
    private ContainerManager containerManager;
    private Map<String, String> filter = new HashMap<>();
    private final Map<String, String> vpcToSecurityGroupMap = new ConcurrentHashMap<>();
    @Autowired
    public AwsRDSPlatform () {
    }
    AwsRDSPlatform (AmazonRDS amazonRDS, AmazonEC2 amazonEC2) {
        this.amazonRDS = amazonRDS;
        this.amazonEC2 = amazonEC2;
    }

    public Map<String, String> getFilter () {
        return filter;
    }

    public Map<String, String> getVpcToSecurityGroupMap () {
        return new HashMap<>(vpcToSecurityGroupMap);
    }


    private Collection<Tag> generateTagsFromFilters () {
        return getFilter().entrySet()
                          .stream()
                          .map(f -> new Tag().withKey(f.getKey()).withValue(f.getValue()))
                          .collect(Collectors.toSet());
    }

    public void setFilter (Map<String, String> filter) {
        this.filter = filter;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void applicationReadyEvent () {
        log.info("Created AWS RDS Platform");
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
        Supplier<Stream<String>> dbInstanceStatusSupplier = () -> getAllDBInstances().stream()
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
        Collection<Container> dbInstanceContainers = getAllDBInstances().stream()
                                                                        .filter(dbInstance -> dbInstance.getDBClusterIdentifier() == null)
                                                                        .map(this::createContainerFromDBInstance)
                                                                        .collect(toSet());
        Collection<Container> dbClusterContainers = getAllDBClusters().stream()
                                                                      .map(this::createContainerFromDBCluster)
                                                                      .collect(toSet());
        return Stream.of(dbClusterContainers, dbInstanceContainers)
                     .flatMap(Collection::stream)
                     .collect(Collectors.toList());
    }

    @Override
    public List<Container> generateExperimentRoster () {
        Map<String, List<AwsContainer>> availabilityZoneMap = getRoster().stream()
                                                                         .map(AwsContainer.class::cast)
                                                                         .collect(groupingBy(AwsContainer::getAvailabilityZone));
        final String[] availabilityZones = availabilityZoneMap.keySet()
                                                              .stream()
                                                              .filter(s -> !s.equals(NO_AZ_INFORMATION))
                                                              .collect(toSet())
                                                              .toArray(new String[]{});
        final String randomAvailabilityZone = availabilityZones[new Random().nextInt(availabilityZones.length)];
        log.debug("Experiment on {} will use {}", keyValue(DATADOG_PLATFORM_KEY, this.getPlatformType()), keyValue(DataDogConstants.AVAILABILITY_ZONE, randomAvailabilityZone));
        List<Container> chosenSet = new ArrayList<>();
        chosenSet.addAll(availabilityZoneMap.get(randomAvailabilityZone));
        chosenSet.addAll(availabilityZoneMap.get(NO_AZ_INFORMATION));
        return chosenSet;
    }

    private Collection<DBCluster> getAllDBClusters () {
        Collection<DBCluster> dbClusters = new HashSet<>();
        DescribeDBClustersRequest describeDBClustersRequest = new DescribeDBClustersRequest();
        DescribeDBClustersResult describeDBClustersResult;
        int i = 0;
        do {
            log.debug("Running describeDBClusters, page {}", ++i);
            describeDBClustersResult = amazonRDS.describeDBClusters(describeDBClustersRequest);
            dbClusters.addAll(describeDBClustersResult.getDBClusters());
            describeDBClustersRequest.setMarker(describeDBClustersResult.getMarker());
        } while (describeDBClustersRequest.getMarker() != null);
        return dbClusters.parallelStream().filter(this::filterDBCluster).collect(Collectors.toSet());
    }

    Boolean filterDBCluster (DBCluster dbCluster) {
        Collection<Tag> tags = generateTagsFromFilters();
        if (tags.isEmpty()) return true;
        ListTagsForResourceResult listTagsForResourceResult = amazonRDS.listTagsForResource(new ListTagsForResourceRequest()
                .withResourceName(dbCluster.getDBClusterArn()));
        List<Tag> tagList = listTagsForResourceResult.getTagList();
        return tagList.containsAll(tags);
    }

    private Collection<DBInstance> getAllDBInstances () {
        Collection<DBInstance> dbInstances = new HashSet<>();
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();
        DescribeDBInstancesResult describeDBInstancesResult;
        int i = 0;
        do {
            log.debug("Running describeDBInstances, page {}", ++i);
            describeDBInstancesResult = amazonRDS.describeDBInstances(describeDBInstancesRequest);
            dbInstances.addAll(describeDBInstancesResult.getDBInstances());
            describeDBInstancesRequest.setMarker(describeDBInstancesResult.getMarker());
        } while (describeDBInstancesRequest.getMarker() != null);
        return dbInstances.parallelStream().filter(this::filterDBInstance).collect(Collectors.toSet());
    }

    boolean filterDBInstance (DBInstance dbInstance) {
        Collection<Tag> tags = generateTagsFromFilters();
        if (tags.isEmpty()) return true;
        ListTagsForResourceResult listTagsForResourceResult = amazonRDS.listTagsForResource(new ListTagsForResourceRequest()
                .withResourceName(dbInstance.getDBInstanceArn()));
        List<Tag> tagList = listTagsForResourceResult.getTagList();
        return tagList.containsAll(tags);
    }

    AwsRDSInstanceContainer createContainerFromDBInstance (DBInstance dbInstance) {
        AwsRDSInstanceContainer container = containerManager.getMatchingContainer(AwsRDSInstanceContainer.class, dbInstance
                .getDBInstanceIdentifier());
        if (container == null) {
            container = AwsRDSInstanceContainer.builder()
                                               .withAwsRDSPlatform(this)
                                               .withAvailabilityZone(dbInstance.getAvailabilityZone())
                                               .withDbInstanceIdentifier(dbInstance.getDBInstanceIdentifier())
                                               .withEngine(dbInstance.getEngine())
                                               .withDbiResourceId(dbInstance.getDbiResourceId())
                                               .build();
            log.debug("Creating RDS Instance Container {} from {}", v(DATADOG_CONTAINER_KEY, container), keyValue("dbInstance", dbInstance));
            containerManager.offer(container);
        } else {
            log.debug("Found existing RDS Instance Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    AwsRDSClusterContainer createContainerFromDBCluster (DBCluster dbCluster) {
        AwsRDSClusterContainer container = containerManager.getMatchingContainer(AwsRDSClusterContainer.class, dbCluster
                .getDBClusterIdentifier());
        if (container == null) {
            container = AwsRDSClusterContainer.builder()
                                              .withAwsRDSPlatform(this)
                                              .withDbClusterIdentifier(dbCluster.getDBClusterIdentifier())
                                              .withEngine(dbCluster.getEngine())
                                              .withDBClusterResourceId(dbCluster.getDbClusterResourceId())
                                              .build();
            log.debug("Created RDS Cluster Container {} from {}", v(DATADOG_CONTAINER_KEY, container), keyValue("dbCluster", dbCluster));
            containerManager.offer(container);
        } else {
            log.debug("Found existing RDS Cluster Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
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
        return dbInstance.getDBInstanceStatus().equals(AwsRDSConstants.AWS_RDS_AVAILABLE) ? NORMAL : RUNNING_EXPERIMENT;
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
        if (!dbCluster.getStatus().equals(AWS_RDS_AVAILABLE)) return RUNNING_EXPERIMENT;
        if (dbCluster.getDBClusterMembers()
                     .stream()
                     .map(DBClusterMember::getDBInstanceIdentifier)
                     .map(this::getDBInstanceHealth)
                     .allMatch(containerHealth -> containerHealth.equals(ContainerHealth.NORMAL))) return NORMAL;
        return RUNNING_EXPERIMENT;
    }

    public void failoverCluster (String dbClusterIdentifier) {
        log.info("Initiating failover request for {}", keyValue(AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier));
        amazonRDS.failoverDBCluster(new FailoverDBClusterRequest().withDBClusterIdentifier(dbClusterIdentifier));
    }

    public void restartInstance (String... dbInstanceIdentifiers) {
        asList(dbInstanceIdentifiers).parallelStream().forEach(this::restartInstance);
    }

    private void restartInstance (String dbInstanceIdentifier) {
        log.info("Initiating Reboot Database Instance request for {}", keyValue(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier));
        amazonRDS.rebootDBInstance(new RebootDBInstanceRequest(dbInstanceIdentifier));
    }

    public Set<String> getClusterInstances (String dbClusterIdentifier) {
        log.info("Getting cluster instances for {}", keyValue(AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier));
        return amazonRDS.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(dbClusterIdentifier))
                        .getDBClusters()
                        .stream()
                        .map(DBCluster::getDBClusterMembers)
                        .flatMap(Collection::stream)
                        .map(DBClusterMember::getDBInstanceIdentifier)
                        .collect(toSet());
    }

    public ContainerHealth getInstanceStatus (String... dbInstanceIdentifiers) {
        log.info("Checking health of instances {}", value("dbInstanceIdentifiers", dbInstanceIdentifiers));
        Collection<ContainerHealth> containerHealthCollection = new HashSet<>();
        for (String dbInstanceIdentifier : dbInstanceIdentifiers) {
            ContainerHealth instanceStatus = getInstanceStatus(dbInstanceIdentifier);
            containerHealthCollection.add(instanceStatus);
            switch (instanceStatus) {
                case NORMAL:
                    log.debug("Container {} returned health {}", value(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier), value("ContainerHealth", instanceStatus));
                    break;
                case DOES_NOT_EXIST:
                case RUNNING_EXPERIMENT:
                    log.warn("Container {} returned health {}", value(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier), value("ContainerHealth", instanceStatus));
                    break;
            }
        }
        return containerHealthCollection.stream()
                                        .max(Comparator.comparingInt(ContainerHealth::getHealthLevel))
                                        .orElse(ContainerHealth.NORMAL);
    }

    private ContainerHealth getInstanceStatus (String dbInstanceIdentifier) {
        List<DBInstance> dbInstances = amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                                                .getDBInstances();
        Supplier<Stream<String>> dbInstanceStatusSupplier = () -> dbInstances.stream()
                                                                             .map(DBInstance::getDBInstanceStatus);
        if (dbInstanceStatusSupplier.get().count() == 0) {
            return ContainerHealth.DOES_NOT_EXIST;
        } else if (dbInstanceStatusSupplier.get().noneMatch(s -> s.equals(AwsRDSConstants.AWS_RDS_AVAILABLE))) {
            return ContainerHealth.RUNNING_EXPERIMENT;
        }
        return ContainerHealth.NORMAL;
    }

    public void setVpcSecurityGroupIds (String dbInstanceIdentifier, String vpcSecurityGroupId) {
        setVpcSecurityGroupIds(dbInstanceIdentifier, Collections.singleton(vpcSecurityGroupId));
    }

    public void setVpcSecurityGroupIds (String dbInstanceIdentifier, Collection<String> vpcSecurityGroupIds) {
        log.info("Setting VPC Security Group ID for {} to {}", value(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier), value(AWS_RDS_VPC_SECURITY_GROUP_ID, vpcSecurityGroupIds));
        try {
            amazonRDS.modifyDBInstance(new ModifyDBInstanceRequest(dbInstanceIdentifier).withVpcSecurityGroupIds(vpcSecurityGroupIds));
        } catch (AmazonRDSException e) {
            if (e.getErrorCode().equals(INVALID_PARAMETER_VALUE)) {
                processInvalidSecurityGroupId(vpcSecurityGroupIds);
            }
            throw new ChaosException(e);
        }
    }

    void processInvalidSecurityGroupId (Collection<String> vpcSecurityGroupIds) {
        vpcToSecurityGroupMap.values().removeAll(vpcSecurityGroupIds);
    }

    ContainerHealth checkVpcSecurityGroupIds (String dbInstanceIdentifier, String vpcSecurityGroupId) {
        return checkVpcSecurityGroupIds(dbInstanceIdentifier, Collections.singleton(vpcSecurityGroupId));
    }

    public ContainerHealth checkVpcSecurityGroupIds (String dbInstanceIdentifier, Collection<String> vpcSecurityGroupIds) {
        Collection<String> actualVpcSecurityGroupIds = getVpcSecurityGroupIds(dbInstanceIdentifier);
        log.info("Comparing VPC Security Group IDs for {}, {}, {}", value(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier), keyValue("expectedVpcSecurityGroupIds", vpcSecurityGroupIds), keyValue("actualSecurityGroupIds", actualVpcSecurityGroupIds));
        return actualVpcSecurityGroupIds.containsAll(vpcSecurityGroupIds) && vpcSecurityGroupIds.containsAll(actualVpcSecurityGroupIds) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
    }

    public Collection<String> getVpcSecurityGroupIds (String dbInstanceIdentifier) {
        return amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                        .getDBInstances()
                        .stream()
                        .map(DBInstance::getVpcSecurityGroups)
                        .flatMap(Collection::stream)
                        .map(VpcSecurityGroupMembership::getVpcSecurityGroupId).collect(toSet());
    }

    public String getChaosSecurityGroup (String dbInstanceIdentifier) {
        String vpcId = getVpcIdOfInstance(dbInstanceIdentifier);
        return vpcToSecurityGroupMap.computeIfAbsent(vpcId, this::getChaosSecurityGroupOfVpc);
    }

    String getVpcIdOfInstance (String dbInstanceIdentifier) {
        return amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                        .getDBInstances()
                        .stream()
                        .map(DBInstance::getDBSubnetGroup)
                        .map(DBSubnetGroup::getVpcId)
                        .findFirst()
                        .orElseThrow(() -> new ChaosException("Could not find a VPC for instance " + dbInstanceIdentifier));
    }

    String getChaosSecurityGroupOfVpc (String vpcId) {
        return amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withFilters(new com.amazonaws.services.ec2.model.Filter("vpc-id")
                .withValues(vpcId)))
                        .getSecurityGroups()
                        .stream()
                        .filter(securityGroup -> securityGroup.getVpcId().equals(vpcId))
                        .filter(securityGroup -> securityGroup.getGroupName()
                                                              .equals(AWS_RDS_CHAOS_SECURITY_GROUP + " " + vpcId))
                        .map(SecurityGroup::getGroupId)
                        .findFirst()
                        .orElseGet(() -> createChaosSecurityGroup(vpcId));
    }

    private String createChaosSecurityGroup (String vpcId) {
        String groupId = amazonEC2.createSecurityGroup(new CreateSecurityGroupRequest().withGroupName(AWS_RDS_CHAOS_SECURITY_GROUP + " " + vpcId)
                                                                                       .withDescription(AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION)
                                                                                       .withVpcId(vpcId)).getGroupId();
        amazonEC2.revokeSecurityGroupEgress(new RevokeSecurityGroupEgressRequest().withGroupId(groupId)
                                                                                  .withIpPermissions(AwsEC2Constants.DEFAULT_IP_PERMISSIONS));
        return groupId;
    }

    public DBSnapshot snapshotDBInstance (String dbInstanceIdentifier) {
        return snapshotDBInstance(dbInstanceIdentifier, false);
    }

    private DBSnapshot snapshotDBInstance (String dbInstanceIdentifier, boolean retry) {
        try {
            CreateDBSnapshotRequest createDBSnapshotRequest = new CreateDBSnapshotRequest().withDBInstanceIdentifier(dbInstanceIdentifier)
                                                                                           .withDBSnapshotIdentifier(getDBSnapshotIdentifier(dbInstanceIdentifier));
            log.info("Requesting an instance snapshot {}", v("CreateDBSnapshotRequest", createDBSnapshotRequest));
            return amazonRDS.createDBSnapshot(createDBSnapshotRequest);
        } catch (DBSnapshotAlreadyExistsException e) {
            log.error("A snapshot by that name already exists", e);
            throw new ChaosException(e);
        } catch (InvalidDBInstanceStateException e) {
            log.error("Cannot snapshot database in an invalid state", e);
            throw new ChaosException(e);
        } catch (DBInstanceNotFoundException e) {
            log.error("DB Instance not found to take snapshot", e);
            throw new ChaosException(e);
        } catch (SnapshotQuotaExceededException e) {
            if (!retry) {
                log.warn("Snapshot failed because quota exceeded. Cleaning old chaos snapshots and retrying");
                cleanupOldSnapshots();
                return snapshotDBInstance(dbInstanceIdentifier, true);
            } else {
                log.error("Exceeded snapshot quota", e);
                throw new ChaosException(e);
            }
        } catch (AmazonRDSException e) {
            log.error("AWS RDS Exception occurred while taking a snapshot", e);
            throw new ChaosException(e);
        } catch (RuntimeException e) {
            log.error("Unknown error occurred while taking a snapshot", e);
            throw new ChaosException(e);
        }
    }

    String getDBSnapshotIdentifier (String dbInstanceIdentifier) {
        String snapshotIdentifier = AwsRDSUtils.generateSnapshotName(dbInstanceIdentifier);
        log.debug("{} will use {}", kv(DataDogConstants.RDS_INSTANCE_ID, dbInstanceIdentifier), kv(DataDogConstants.RDS_INSTANCE_SNAPSHOT, snapshotIdentifier));
        return snapshotIdentifier;

    }

    public DBClusterSnapshot snapshotDBCluster (String dbClusterIdentifier) {
        return snapshotDBCluster(dbClusterIdentifier, false);
    }

    private DBClusterSnapshot snapshotDBCluster (String dbClusterIdentifier, boolean retry) {
        try {
            CreateDBClusterSnapshotRequest createDBClusterSnapshotRequest = new CreateDBClusterSnapshotRequest().withDBClusterIdentifier(dbClusterIdentifier)
                                                                                                                .withDBClusterSnapshotIdentifier(getDBSnapshotIdentifier(dbClusterIdentifier));
            log.info("Requesting a cluster snapshot {}", v("CreateDBClusterSnapshotRequest", createDBClusterSnapshotRequest));
            return amazonRDS.createDBClusterSnapshot(createDBClusterSnapshotRequest);
        } catch (DBClusterSnapshotAlreadyExistsException e) {
            log.error("A cluster snapshot by that name already exists", e);
            throw new ChaosException(e);
        } catch (InvalidDBClusterStateException e) {
            log.error("DB Cluster is in invalid state for snapshot", e);
            throw new ChaosException(e);
        } catch (DBClusterNotFoundException e) {
            log.error("DB Cluster not found to take snapshot", e);
            throw new ChaosException(e);
        } catch (SnapshotQuotaExceededException e) {
            if (!retry) {
                log.warn("Snapshot failed because quota exceeded. Cleaning old chaos snapshots and retrying");
                cleanupOldSnapshots();
                return snapshotDBCluster(dbClusterIdentifier, true);
            } else {
                log.error("Exceeded snapshot quota", e);
                throw new ChaosException(e);
            }
        } catch (InvalidDBClusterSnapshotStateException e) {
            log.error("DB Cluster Snapshot in invalid state", e);
            throw new ChaosException(e);
        } catch (AmazonRDSException e) {
            log.error("AWS RDS Exception occurred while taking a snapshot", e);
            throw new ChaosException(e);
        } catch (RuntimeException e) {
            log.error("Unknown error occurred while taking a snapshot", e);
            throw new ChaosException(e);
        }
    }

    // Don't run for the first hour, but run every 15 minute afterwards/
    @Scheduled(initialDelay = 1000L * 60 * 60, fixedDelay = 1000L * 60 * 15)
    void cleanupOldSnapshots () {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(DataDogConstants.DATADOG_PLATFORM_KEY, this.getPlatformType())) {
            log.info("Cleaning up old snapshots");
            cleanupOldSnapshots(60);
        }
    }

    void cleanupOldSnapshots (int olderThanMinutes) {
        log.info("Clearing old chaos snapshots older than {}", v("oldSnapshotTime", Duration.ofMinutes(olderThanMinutes)));
        cleanupOldInstanceSnapshots(olderThanMinutes);
        cleanupOldClusterSnapshots(olderThanMinutes);
    }

    void cleanupOldInstanceSnapshots (int olderThanMinutes) {
        amazonRDS.describeDBSnapshots()
                 .getDBSnapshots()
                 .stream()
                 .filter(dbSnapshot -> AwsRDSUtils.isChaosSnapshot(dbSnapshot.getDBSnapshotIdentifier()))
                 .filter(dbSnapshot -> snapshotIsOlderThan(dbSnapshot.getDBSnapshotIdentifier(), olderThanMinutes))
                 .peek(dbSnapshot -> log.info("Deleting instance snapshot {} since it is out of date", v("dbSnapshot", dbSnapshot)))
                 .parallel()
                 .forEach(this::deleteInstanceSnapshot);
    }

    boolean snapshotIsOlderThan (String dbSnapshotName, int olderThanMinutes) {
        Matcher m = CalendarUtils.datePattern.matcher(dbSnapshotName);
        if (m.find()) {
            String dateSection = m.group(1);
            Instant snapshotTime = AwsRDSUtils.getInstantFromNameSegment(dateSection);
            log.debug("Calculated snapshot {} has a timestamp of {}", v(DataDogConstants.RDS_INSTANCE_SNAPSHOT, dbSnapshotName), v("snapshotTime", snapshotTime));
            return snapshotTime.plus(Duration.ofMinutes(olderThanMinutes)).isBefore(Instant.now());
        }
        log.warn("Could not extract a timestamp from {}.", v(DataDogConstants.RDS_INSTANCE_SNAPSHOT, dbSnapshotName));
        return false;
    }

    void cleanupOldClusterSnapshots (int olderThanMinutes) {
        amazonRDS.describeDBClusterSnapshots()
                 .getDBClusterSnapshots()
                 .stream()
                 .filter(dbClusterSnapshot -> AwsRDSUtils.isChaosSnapshot(dbClusterSnapshot.getDBClusterSnapshotIdentifier()))
                 .filter(dbClusterSnapshot -> snapshotIsOlderThan(dbClusterSnapshot.getDBClusterSnapshotIdentifier(), olderThanMinutes))
                 .peek(dbClusterSnapshot -> log.info("Deleting cluster snapshot {} since it is out of date", v("dbClusterSnapshot", dbClusterSnapshot)))
                 .parallel()
                 .forEach(this::deleteClusterSnapshot);
    }

    public void deleteInstanceSnapshot (DBSnapshot dbSnapshot) {
        log.info("Deleting RDS Instance Snapshot {}", v(DataDogConstants.RDS_INSTANCE_SNAPSHOT, dbSnapshot));
        amazonRDS.deleteDBSnapshot(new DeleteDBSnapshotRequest().withDBSnapshotIdentifier(dbSnapshot.getDBSnapshotIdentifier()));
    }

    public void deleteClusterSnapshot (DBClusterSnapshot dbClusterSnapshot) {
        log.info("Deleting RDS Cluster Snapshot {}", v(DataDogConstants.RDS_CLUSTER_SNAPSHOT, dbClusterSnapshot));
        amazonRDS.deleteDBClusterSnapshot(new DeleteDBClusterSnapshotRequest().withDBClusterSnapshotIdentifier(dbClusterSnapshot
                .getDBClusterSnapshotIdentifier()));
    }

    public boolean isInstanceSnapshotRunning (String dbInstanceIdentifier) {
        log.debug("Evaluating if {} is backing up", kv(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier));
        boolean snapshotRunning = amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier))
                                           .getDBInstances()
                                           .stream()
                                           .anyMatch(dbInstance -> dbInstance.getDBInstanceStatus()
                                                                             .equals(AWS_RDS_BACKING_UP));
        log.info("{} is backing up = {}", kv(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier), v("backupInProgress", snapshotRunning));
        return snapshotRunning;
    }

    public boolean isClusterSnapshotRunning (String dbClusterIdentifier) {
        log.debug("Evaluating if {} is backing up", kv(AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier));
        boolean snapshotRunning = amazonRDS.describeDBClusters(new DescribeDBClustersRequest().withDBClusterIdentifier(dbClusterIdentifier))
                                           .getDBClusters()
                                           .stream()
                                           .anyMatch(dbCluster -> dbCluster.getStatus().equals(AWS_RDS_BACKING_UP));
        log.info("{} is backing up = {}", kv(AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier), v("backupInProgress", snapshotRunning));
        return snapshotRunning;
    }
}
