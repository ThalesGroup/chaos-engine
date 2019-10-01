# AWS EC2 Experiments

## SDK

The AWS Module makes use of the Official [AWS SDK for Java] from Amazon.
### Version
The current SDK Version in use is 1.11.357.

## Configuration
|	 Key Name 	|	 Description 	|	 Default 	|	
|	 --- 	|	 --- 	|	 :---: 	|	
|	`aws.ec2`	|	The presence of this key enables the EC2 Module	|	N/A	|	
|	`aws.accessKeyId`	|	The Access Key ID for an AWS API Key	|	None	|	
|	`aws.secretAccessKey`	|	The Access Key Secret for the given Access Key ID	|	None	|	
|	`aws.region`	|	The AWS Region to run experiments on.	|	**us-east-2**	|	
|	`aws.ec2.groupingTags`	|	A comma separated list of tag names to use as a shared resource identifier. See [Autoscaling Support](#autoscaling-support) for more information.	|	None	|	
|	`aws.ec2.filter.<key>`	|	Each key/value pair will be used to restrict the scope of Experiment targets. See [Filtering](#filtering) for more information.	|	None	|	
|	`aws.ec2.sshprivatekeys.<key>`	|	Each key/value pair will be used to associate an EC2 SSH Key and confirm SSH access. See [SSH Based Experiments](#ssh-experiment-support) for more information.	|	None	|	
|	`aws.ec2.imageIdToUsernameMap.<image-id>`	|	 Associate EC2 AMI Image IDs to the username used for SSH Access 	|	**ec2-user**	|	
|	`aws.ec2.routableCidrBlocks`	|	 A comma separated list of CIDR Blocks that should be considered Routable for SSH Access. 	|	 None 	|	

!!! note
    The `aws.accessKeyId`, `aws.secretAccessKey`, and `aws.region` keys are shared with other AWS Modules.

## Node Discovery

The AWS EC2 Platform automatically discovers EC2 containers that the provided keys are able to access. If any filters are configured, those filter values are set. It will also filter out it's own instance if found to be running in EC2 (see [Self Awareness](#self-awareness)).

### Filtering

Each key/value pair under the `aws.ec2.filter.<key>` map will be used for filtering AWS EC2 Instances to find the scope of Chaos. The specific criteria are the same as the [AWS CLI]. However, since environment variables cannot contain dashes (-), functionality was added to parse **camelCaps** into **dash-separated-words**. For example, to use the filter criteria **root-device-name**, set the property **_aws.ec2.filter_.rootDeviceName**.

#### Tags

To filter on a tag, use the subkey **tag.tag_name**. For example, to filter on instances where the tag **service_name** is **mongo**, use the property **_aws.ec2.filter_.tag.service_name=mongo**. The same **camelCaps** conversion does not apply.

### Self Awareness

The Chaos Engine will detect if it is running in an EC2 instance, and will avoid itself as a potential target.

The mechanism calls the magic AWS EC2 URI <http://169.254.169.254/latest/meta-data/instance-id>. If running in EC2, this will return the Instance ID of the machine. If not running in EC2, this IP Address should not respond, as it is an RFC 3927 address.

## Autoscaling Support

The EC2 Module makes use of AWS Autoscaling Groups in three capacities. 
* As a mechanism to ensure that not all identical services are experimented on simultaneously
* As a mechanism of determining if an EC2 instance has returned to a full healthy state, including potential replacement containers
* As a mechanism to initiate self healing on an EC2 Instance.

### Discovery Mechanism

The property **aws.ec2.groupingTags** is used to identify which tags should be used for considering grouped instances. By default this is just _aws:autoscaling:groupName_, but if you are using other technologies that operate in a similar mechanism, you can provide a full comma separated list of all tags in the order of preferred use.

#### Experiment Roster Generation

At the beginning of every EC2 based experiment set, for each group, if at least 2 instances exist in that group, one will be flagged as a designated survivor. That instance will be immune from direct experimentation during this time. If a scaling group tag is not found for an instance, or if only one instance exists for a scaling group, then there is no guaranteed protection from experiments. In a real world scenario, these individual instances are just as likely to fail as any single redundant instance, and so are considered eligible for experimentation.

#### Health Check Mechanism

Autoscaled EC2 instances also handle their experiment results differently. An EC2 instance as part of an autoscaling group can also complete any experiment by entering a Terminated state. This state indicates that the mechanism for managing these instances has recognized an issue and is recreating the instance.

If the instance uses AWS Autoscaling as described above, the system also verifies that the autoscaling group has a number of healthy instances equal to its number of desired instances. The experiment is not considered finished until the autoscaling group is back until desired capacity.

API: <https://docs.aws.amazon.com/autoscaling/ec2/APIReference/API_DescribeAutoScalingGroups.html>

#### Self Healing Mechanism

EC2 instances managed by AWS Autoscaling Groups will use an alternative method for their FIRST execution of self healing. Instead of using whatever self healing mechanism there is, the engine will use the AWS Autoscaling Group API and mark the instance as Unhealthy. The Autoscaling Group should pick this up and recreate the instance.

API: <https://docs.aws.amazon.com/autoscaling/ec2/APIReference/API_SetInstanceHealth.html>

## SSH Experiment Support

EC2 Instances will be considered for SSH Based Experiments based on two criteria. The first criteria is that the property **aws.ec2.sshprivatekeys.<keyname>** must be set, where **keyname** is the name of the SSH Keypair in AWS of the instance. That value should contain the SSH Private Key used for the connection.

The second criteria is that the EC2 instance must be routable. This is handled by checking if either the instance has a Public IP Address, or if its private IP Address is contained in a CIDR Block configured via the **aws.ec2.routableCidrBlocks** variable. When connecting, a routable private IP Address is preferred over a public IP Address.

Connections are made using the username in the configurable map of **aws.ec2.imageIdToUsernameMap**, where each expected subkey is an ImageID, and its value is a username for that AMI. Any AMI that is not present in the map will use the username **ec2-user**.

See [SSH Based Experiments] for details on what experiments may be run.

## Experiment Methods

### Instance Stopped

Stopping an EC2 instance tests both the ability of the PaaS solution to move instances over to other hosts, and also tests that the PaaS solution has not overloaded a critical amount of a microservice instances into one host.

#### Mechanism

API: [API StopInstances]

The StopInstances API is called with Force = True. This overrides the setting to prevent accidental stops of EC2 instances.

##### Health Check

API: [API DescribeInstances]

The DescribeInstances API is called and parsed for the InstanceState of the specific InstanceID. A healthy instance is in a running state (Code 16).

##### Self Healing

API: [API StartInstances]

The StartInstances API is called when the experiment has reached it's conclusion. This should result in the container being in a running state, allowing the Health Check to pass.

### Instance Rebooted

Rebooting an EC2 instance tests that the PaaS solution effectively moves instances over to other hosts, but also tests that the PaaS solution moves the instances back after the reboot has finalized, allowing it to take benefit of all available resources.

#### Mechanism

API: [API RebootInstances]

The RebootInstances API is called. This API causes a Ctrl+Alt+Del to be triggered in the instance.

??? warning "Ctrl+Alt+Del vs Hard Reboot"
    Some AMI's have Ctrl+Alt+Del disabled. In these cases, a Hard Reboot is initiated by AWS 4 minutes after the Reboot Request was made.

#### Health Check

API: [API DescribeInstances]

An internal timer is started when the Reboot command is run. After 4 minutes, the DescribeInstances API is used to verify that the instance is running. This validates that, either a graceful reboot occurred under 4 minutes, or after 4 minutes, if a hard reboot was performed, the instance came back in a running state.

#### Self Healing

API: [API StartInstances]

The StartInstances API is called when the experiment has reached it's conclusion. This should result in the container being in a running state, allowing the Health Check to pass.

### Security Groups Changed

Changing the security groups of an EC2 instance will remove it from network accessibility. This tests that the PaaS system is able to properly balance work on nodes that are still connected to the network, and that the disconnected nodes do not cause consistency errors when they are able to reconnect.

####Mechanism

API: [API DescribeInstanceAttribute], [API ModifyInstanceAttribute]

The original security groups for an instance are recorded using the DescribeInstanceAttribute API. The security group is then replaced with an automatically created Chaos Security Group using the ModifyInstanceAttribute API.

#### Health Check

API: [API DescribeInstanceAttribute]

The security groups returned in the DescribeInstanceAttribute API are compared to the list preserved during the experiment initiation. If the two lists are exclusively equal, the system is deemed healthy.

#### Self Healing

API: [API ModifyInstanceAttribute]

The original security groups are put back in place using the ModifyInstanceAttribute API.

#### Automatic Security Group Creation Mechanism

API: [API DescribeSecurityGroups], [API CreateSecurityGroup], [API DescribeVpcs]

The Default VPC is looked up using the DescribeVpcs API, and cached. The Security Groups are looked up using the DescribeSecurityGroup API, and parsed for a security group named ChaosEngine Security Group. If one is found, it is cached and used. If it is not found, it is created using the CreateSecurityGroup API, and cached for use.



### Terminate Instance in Auto Scaling Group

Terminating an instance in an Auto Scaling Group tests the overall resolution time of an error, ensuring the overall customer experience is not impacted. Additionally, it verifies if the product returns back to it's steady state when the requested amount of instances is restored.

####Mechanism

API: [API TerminateInstances]

The TerminateInstances API is called with the InstanceID under test as parameter.

#### Health Check

API: [API DescribeAutoScalingGroups]

The health check proves if the actual capacity is back to the desired capacity inside the given Auto Scaling Group.

#### Self Healing

As a terminated instance cannot be restored, no self healing mechanism has been implemented. Therefore, an outcome of this experiment can be that the steady state cannot be restored


[API StartInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_StartInstances.html
[API DescribeInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
[API StopInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_StopInstances.html
[API RebootInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_RebootInstances.html
[API DescribeInstanceAttribute]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstanceAttribute.html
[API ModifyInstanceAttribute]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_ModifyInstanceAttribute.html
[API DescribeSecurityGroups]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeSecurityGroups.html
[API CreateSecurityGroup]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_CreateSecurityGroup.html
[API DescribeVpcs]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeVpcs.html
[API TerminateInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_TerminateInstances.html
[API DescribeAutoScalingGroups]: https://docs.aws.amazon.com/autoscaling/ec2/APIReference/API_DescribeAutoScalingGroups.html

[AWS SDK for Java]: https://aws.amazon.com/sdk-for-java/
[AWS CLI]: https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instances.html#options
[SSH Based Experiments]: ./ssh_based_experiments.md