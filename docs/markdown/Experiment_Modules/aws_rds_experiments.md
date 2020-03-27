# AWS RDS Experiments 

## SDK

The AWS Module makes use of the AWS SDK from Amazon.

<https://aws.amazon.com/sdk-for-java/>

Version: 1.11.357

## Configuration

Several environment variables control how AWS EC2 is instantiated. ** The presence of *Some Variables* control whether or not the module is loaded.

| Key Name | Description | Default | Mandatory |
| --- | --- | --- |	:---: 	|
| `aws.rds` | The presence of this key controls if this module is loaded. | N/A | Yes |
| `aws.accessKeyId` | The Access Key ID for an AWS API key. This key is shared with other AWS Modules. | None | Yes |
| `aws.secretAccessKey` | The Access Key Secret associated to the same AWS API Key. This key is shared with other AWS Modules. | None | Yes |
| `aws.region` | The AWS Region to run experiments in. Formatted like: **eu-central-1**. This key is shared with other AWS Modules. | us-east-2 | Yes |
| `aws.rds.filter.<tag_name>` | Each key will be used for filtering nodes. See Node Discovery for more information. e.g. *aws.rds.filter.ChaosVictim*=*true* | N/A | Yes |

## Node Discovery

The AWS RDS Module discovers two sets of Containers. The first set represents AWS DB Clusters, and the second represents AWS DB Instances not part of the original clusters.

### Mechanism

API: [API DescribeDBInstances], [API DescribeDBClusters]

The results of both API calls are parsed into their appropriate objects. DB Instances that are flagged as being a cluster member are discarded.

When needed, a property of the Clusters, the DBClusterMembers, are parsed to find instances that belong to that cluster.

### Filtering

API: [API ListTagsForResource]

The sub-properties of **aws.rds.filter** are used for creating required tag filters for Instances and Clusters. For example, if the environment variable **aws.rds.filter.exampleTag** was set to **true**, all instances and clusters that do not contain the tag **exampleTag** with value **true** will be excluded. The ListTagsForResource API is used to get the tags of instances and clusters, and is only invoked if there are tags set. 

## Experimentation Roster

Because RDS is treated as a SAAS solution, and a certain amount of redundancy is expected to be handled by the platform, we intentionally do not experiment on all replicas of given containers, as it is unlikely for such a scenario to occur without a complete region outage. To keep our experiments fair with what real world scenarios we can reasonably expect, when an experiment is ran against RDS, a single Availability Zone that is in use is selected, and experiments are isolated to that AZ.

## Cluster Experiments

### Instance Rebooted

Rebooting a number of instances in an RDS Cluster should be completely seemless, as there are still a number of instances available to handle requests. This may cause some delays in processing a larger amount of requests, which may cause issues upstream from the request (i.e., low CPU usage on application servers as they wait for DB response triggering autoscaling). Because of the nature of clusters, the experiment will choose a random number of instances in a cluster to reboot, such that at least 1 will be rebooted, but at most N-1 will be rebooted.

#### Mechanism

API: [API RebootDBInstance]

The RebootDBInstance API is called once for every DB Instance to be rebooted in the cluster.

##### Health Check

API: [API DescribeDBInstances]

The results of DescribeDBInstances are parsed for all instances in the experiment. If all instance statuses are "available", the experiment is finished.

##### Self Healing

The system is completely capable of self healing from the AWS Perspective. This experiment only tests stability of systems that rely on the databases, and not the database system itself.

### Failover Initiated

A failover may occur in an RDS Cluster at any time. That failover may be part of a larger system failure. By initiating the failover independently, we can be sure that the failover reconciliation does not cause an issue downstream if and when it occurs naturally.

#### Mechanism

API:[API FailoverDBCluster]

The FailoverDBCluster API is called for the DB Cluster Identifier of the container.

##### Health Check

API: [API DescribeDBInstances]

The results of DescribeDBInstances are parsed for all instances in the cluster. If all instance statuses are "available", the experiment is finished.

##### Self Healing

The system is completely capable of self healing from the AWS Perspective. This experiment only tests stability of systems that rely on the databases, and not the database system itself.

### Backup In Progress

Initiating a backup of a database may cause extra latency, database locks, or other odd behaviour. Normally backups should be scheduled to occur during a low usage maintenance period, but there are several scenarios where they may occur during normal operations. This includes manually initiated backups prior to a Change Request being fulfilled, or services that don't actually have periods of complete downtime.

#### Mechanism

API: [API CreateDBClusterSnapshot]

The CreateDBClusterSnapshot API is called for the specific DB Cluster. A snapshot identifier is generated using the DB Cluster Identifier, such that it will be named chapsSnapshot-{DBClusterIdentifier}-yyyy-MM-ddtHH-mm-ss\[-sss\]Z. The name will automatically trim characters from the Instance Identifier to fit the 255 character limit, and will replace all disallowed characters with `-`, and trim all repeated `-`'s.

In the event that a snapshot is failed to create due to Snapshot Quota being exceeded, the DeleteDBClusterSnapshot and DeleteDBSnapshot API's are called for any old Chaos snapshots that were not automatically cleaned up after their experiment. The experiment will be retried once.

##### Health Check

API: [API DescribeDBClusters]

A DescribeDBClusters request is run against the Cluster ID being experimented on. If the state is "backing-up", then the experiment is still in progress.

##### Self Healing

API: [API DeleteDBClusterSnapshot]

If the experiment takes too long, the DeleteDBClusterSnapshot API is called. Deleting a snapshot in progress causes the backup to be cancelled.

##### Finalization

API: [API DeleteDBClusterSnapshot]

Once the experiment is over, the DeleteDBClusterSnapshot API is called to delete the snapshot.

## Instance Experiments

### Instance Rebooted

Rebooting an instance outside of a cluster tests a random failure of the instance. AWS is expected to bring the resource back up, but we need to ensure that applications dependent on the database are not negatively impacted during the downtime.

#### Mechanism

API: [API RebootDBInstance]

The Reboot DB Instance API is called for the individual instance Identifier.

##### Health Check

API: [API DescribeDBInstances]

The Describe DB Instance API is called for the individual instance identifier. If the instance status is "available", the experiment is finished.

##### Self Healing

The system is completely capable of self healing from the AWS Perspective. This experiment only tests stability of systems that rely on the databases, and not the database system itself.

### Security Groups Changed

Changing the Security Groups on a database tests what happens in applications that depend on the database when a network failure occurs while connecting. Unlike the Instance Rebooted test, this does not necessarily have a graceful disconnect from all existing resources. In addition, triggers in the database may still occur, and transactions may be held open.

#### Mechanism

API: [API DescribeDBInstances], [API ModifyDBInstance]

The DescribeDBInstances API is called to pull an existing list of Security Groups associated to a database instance. After looking up (and creating, if necessary) a security group to move the database into, the change is done using the modifyDBInstance API Call.

##### Health Check

API: [API DescribeDBInstances]

The DescribeDBInstances API call is used to get the list of actual Security Groups applied to instances. This list is compared to the list recorded at the start of the experiment, and if they are exactly equal, the experiment is finished.

##### Self Healing

API: [API ModifyDBInstance]

The ModifyDBInstance API call is used to revert the Security Groups of the DB Instance back to the original values.

#### Automatic Security Group Creation Mechanism

API: [API DescribeSecurityGroups], [API CreateSecurityGroup], [API DescribeVpcs]

The Chaos Security Group creation requires the AWS EC2 module to load. The Default VPC is looked up using the DescribeVpcs API, and cached. The Security Groups are looked up using the DescribeSecurityGroup API, and parsed for a security group named *ChaosEngine Security Group*. If one is found, it is cached and used. If it is not found, it is created using the CreateSecurityGroup API, and cached for use.

### Backup In Progress

Initiating a backup of a database may cause extra latency, database locks, or other odd behaviour. Normally backups should be scheduled to occur during a low usage maintenance period, but there are several scenarios where they may occur during normal operations. This includes manually initiated backups prior to a Change Request being fulfilled, or services that don't actually have periods of complete downtime.

#### Mechanism

API: [API CreateDBSnapshot]

The CreateDBSnapshot API is called for the specific DB Instance. A snapshot identifier is generated using the DB Instance Identifier, such that it will be named **chapsSnapshot-{DBInstanceIdentifier}-yyyy-MM-ddtHH-mm-ss\[-sss\]Z**. The name will automatically trim characters from the Instance Identifier to fit the 255 character limit, and will replace all disallowed characters with **-**, and trim all repeated **-**'s to a single one.

In the event that a snapshot is failed to create due to Snapshot Quota being exceeded, the DeleteDBClusterSnapshot and DeleteDBSnapshot API's are called for any old Chaos snapshots that were not automatically cleaned up after their experiment.

##### Health Check

API:[API DescribeDBInstances]

A DescribeDBInstances request is run against the instance ID being experimented on. If the state is "backing-up", then the experiment is still in progress.

##### Self Healing

API: [API DeleteDBSnapshot]

If the experiment takes too long, the DeleteDBSnapshot API is called. Deleting a snapshot in progress causes the backup to be cancelled.

##### Finalization

API: [API DeleteDBSnapshot]

Once the experiment is over, the DeleteDBSnapshot API is called to delete the snapshot.

## Health Check

When the Health Check endpoint of the Chaos Engine is polled, it will test if the AWS RDS API is returning without errors. It does so by running a DescribeDBInstances API and DescribeDBClusters, and discarding the results. If any exceptions are thrown, the API Status returns as failed, allowing any Health Check mechanisms for the Chaos Engine to run.

## Snapshot Cleanup

API: [API DescribeDBSnapshots], [API DeleteDBSnapshot], [API DescribeDBClusterSnapshots], [API DeleteDBClusterSnapshot]

Snapshots created by Chaos Engine are supposed to be cleaned up as part of their experiments. In the event that an experiment fails to do so, an automated mechanism will try and delete any snapshots that are named with the Chaos Engine's snapshot naming convention (ChaosSnapshot-&lt;Instance Nane&gt;-&lt;Date/Time&gt;). The snapshots are listed using the DescribeDBSnapshots and DescribeDBClusterSnapshots API's, and any that are more than 60 minutes old are deleted using the DeleteDBSnapshot and DeleteDBClusterSnapshot APIs.

[API CreateDBClusterSnapshot]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBClusterSnapshot.html
[API CreateDBSnapshot]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBSnapshot.html
[API DeleteDBClusterSnapshot]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DeleteDBClusterSnapshot.html
[API DeleteDBSnapshot]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DeleteDBSnapshot.html
[API DescribeDBClusters]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DescribeDBClusters.html
[API DescribeDBClusterSnapshots]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DescribeDBClusterSnapshots.html
[API DescribeDBInstances]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DescribeDBInstances.html
[API DescribeDBSnapshots]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DescribeDBSnapshots.html
[API FailoverDBCluster]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_FailoverDBCluster.html
[API ListTagsForResource]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_ListTagsForResource.html
[API ModifyDBInstance]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_ModifyDBInstance.html
[API RebootDBInstance]: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_RebootDBInstance.html
[API CreateSecurityGroup]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_CreateSecurityGroup.html
[API DescribeSecurityGroups]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeSecurityGroups.html
[API DescribeVpcs]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeVpcs.html
