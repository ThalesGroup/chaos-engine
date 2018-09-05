package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.AwsEC2SelfAwareness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME;

@Component
@ConditionalOnProperty({ "aws.ec2.accessKeyId", "aws.ec2.secretAccessKey" })
public class AwsEC2Platform extends Platform {
    private AmazonEC2 amazonEC2;
    private ContainerManager containerManager;
    private Map<String, String> filter = new HashMap<>();
    private AwsEC2SelfAwareness awsEC2SelfAwareness;
    private String chaosSecurityGroupId;
    private Vpc defaultVpc;

    @Autowired
    AwsEC2Platform (@Value("${AWS_FILTER_KEYS:#{null}}") String[] filterKeys, @Value("${AWS_FILTER_VALUES:#{null}}") String[] filterValues, AmazonEC2 amazonEC2, ContainerManager containerManager, AwsEC2SelfAwareness awsEC2SelfAwareness) {
        this();
        if (filterKeys != null && filterValues != null) {
            if (filterKeys.length != filterValues.length) {
                throw new ChaosException("Cannot start with an unequal amount of Filter Keys and Values");
            }
            for (int i = 0; i < filterKeys.length; i++) {
                filter.putIfAbsent(filterKeys[i], filterValues[i]);
            }
        }
        this.amazonEC2 = amazonEC2;
        this.containerManager = containerManager;
        this.awsEC2SelfAwareness = awsEC2SelfAwareness;
    }

    private AwsEC2Platform () {
        log.info("AWS EC2 Platform created");
    }

    /**
     * Runs an API call and tests that it returns without issue. Any exceptions returns an API Error
     *
     * @return OK if call resolves, or ERROR if the call fails.
     */
    @Override
    public ApiStatus getApiStatus () {
        try {
            amazonEC2.describeInstances();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            log.error("API for AWS EC2 failed to resolve.", e);
            return ApiStatus.ERROR;
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.IAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        Stream<Instance> instances = getInstanceStream();
        Set<InstanceState> instanceStates = instances.map(Instance::getState).collect(Collectors.toSet());
        Set<Integer> instanceStateCodes = instanceStates.stream()
                                                        .map(InstanceState::getCode)
                                                        .collect(Collectors.toSet());
        for (int state : AwsEC2Constants.getAwsUnhealthyCodes()) {
            if (instanceStateCodes.contains(state)) return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
    }

    @Override
    public List<Container> generateRoster () {
        return generateRosterImpl(filter);
    }

    /**
     * Polls AWS EC2 API for a complete list of instances, with optional filters, and returns a list of Container class
     * objects to be used.
     *
     * @param filterValues Key/Value pair of tags to do an inclusive filter on
     * @return A list of Containers representing EC2 instances.
     */
    private List<Container> generateRosterImpl (Map<String, String> filterValues) {
        final List<Container> containerList = new ArrayList<>();
        Collection<Filter> filters = new HashSet<>();
        if (filterValues != null) {
            filterValues.forEach((k, v) -> filters.add(new Filter().withName("tag:" + k).withValues(v)));
        }
        boolean done = false;
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withFilters(filters);
        while (!done) {
            DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
            describeInstancesResult.getReservations()
                                   .parallelStream()
                                   .map(Reservation::getInstances)
                                   .flatMap(Collection::parallelStream)
                                   .filter(instance -> !awsEC2SelfAwareness.isMe(instance.getInstanceId()))
                                   .forEach(instance -> createContainerFromInstance(containerList, instance));
            describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
            if (describeInstancesRequest.getNextToken() == null) {
                done = true;
                // Loops until all pages of instances have been resolved
            }
        }
        return containerList;
    }

    /**
     * Creates a Container object from an EC2 Instance and appends it to a provided list of containers.
     *
     * @param containerList The list of containers to append the finalized container into
     * @param instance      An EC2 Instance object to have a container created.
     */
    private void createContainerFromInstance (List<Container> containerList, Instance instance) {
        if (instance.getState().getCode() == AwsEC2Constants.AWS_TERMINATED_CODE) return;
        Container newContainer = createContainerFromInstance(instance);
        Container container = containerManager.getOrCreatePersistentContainer(newContainer);
        if (container != newContainer) {
            log.debug("Found existing container {}", container);
        } else {
            log.info("Found new container {}", container);
        }
        containerList.add(container);
    }

    /**
     * Creates a Container object given an EC2 Instance object.
     *
     * @param instance Instance to have the Container created from.
     * @return Container mapping to the instance.
     */
    private Container createContainerFromInstance (Instance instance) {
        String name;
        Optional<Tag> nameTag = instance.getTags().stream().filter(tag -> tag.getKey().equals("Name")).findFirst();
        // Need to use an Optional for the name tag, as it may not be present.
        name = nameTag.isPresent() ? nameTag.get().getValue() : "no-name";
        return AwsEC2Container.builder().awsEC2Platform(this)
                              .instanceId(instance.getInstanceId())
                              .keyName(instance.getKeyName())
                              .name(name)
                              .build();
    }

    private Stream<Instance> getInstanceStream () {
        return getInstanceStream(new DescribeInstancesRequest());
    }

    private Stream<Instance> getInstanceStream (DescribeInstancesRequest describeInstancesRequest) {
        return amazonEC2.describeInstances(describeInstancesRequest)
                        .getReservations()
                        .stream()
                        .map(Reservation::getInstances)
                        .flatMap(List::stream);
    }

    public ContainerHealth checkHealth (String instanceId) {
        Instance instance;
        InstanceState state;
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult result = amazonEC2.describeInstances(request);
        try {
            instance = result.getReservations().get(0).getInstances().get(0);
            state = instance.getState();
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            // If Index 0 in array doesn't exist, or we get an NPE, it's because the instance doesn't exist anymore.
            log.error("Instance {} doesn't seem to exist anymore", instanceId, e);
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return state.getCode() == AwsEC2Constants.AWS_RUNNING_CODE ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK;
    }

    public void stopInstance (String... instanceIds) {
        log.info("Requesting a stop of instances {}", (Object[]) instanceIds);
        amazonEC2.stopInstances(new StopInstancesRequest().withForce(true).withInstanceIds(instanceIds));
    }

    void terminateInstance (String... instanceIds) {
        log.info("Requesting a Terminate of instances {}", (Object[]) instanceIds);
        amazonEC2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
    }

    public void startInstance (String... instanceIds) {
        log.info("Requesting a start of instances {}", (Object[]) instanceIds);
        amazonEC2.startInstances(new StartInstancesRequest().withInstanceIds(instanceIds));
    }

    public void restartInstance (String... instanceIds) {
        log.info("Requesting a reboot of instances {}", (Object[]) instanceIds);
        amazonEC2.rebootInstances(new RebootInstancesRequest().withInstanceIds(instanceIds));
    }

    public void setSecurityGroupIds (String instanceId, List<String> securityGroupIds) {
        amazonEC2.modifyInstanceAttribute(new ModifyInstanceAttributeRequest().withInstanceId(instanceId)
                                                                              .withGroups(securityGroupIds));
    }

    public String getChaosSecurityGroupId () {
        if (chaosSecurityGroupId == null) {
            // Let's cache this value, only need to look it up once.
            initChaosSecurityGroupId();
        }
        return chaosSecurityGroupId;
    }

    void initChaosSecurityGroupId () {
        amazonEC2.describeSecurityGroups()
                 .getSecurityGroups()
                 .stream()
                 // Our security group must exist in the default VPC.
                 .filter(securityGroup -> securityGroup.getVpcId().equals(getDefaultVPC()))
                 // Our Security Group is identified by a static name.
                 .filter(securityGroup -> securityGroup.getGroupName().equals(EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME))
                 .findFirst()
                 // If present, set the value, skipping the next section.
                 .ifPresent(securityGroup -> chaosSecurityGroupId = securityGroup.getGroupId());
        if (chaosSecurityGroupId == null) {
            chaosSecurityGroupId = createChaosSecurityGroup();
        }
    }

    private String getDefaultVPC () {
        initDefaultVpc();
        return defaultVpc != null ? defaultVpc.getVpcId() : null;
    }

    private String createChaosSecurityGroup () {
        return amazonEC2.createSecurityGroup(new CreateSecurityGroupRequest().withGroupName(EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME)
                                                                             .withVpcId(getDefaultVPC())
                                                                             .withDescription(AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_DESCRIPTION))
                        .getGroupId();
    }

    private synchronized void initDefaultVpc () {
        if (defaultVpc != null) return;
        defaultVpc = amazonEC2.describeVpcs().getVpcs().stream().filter(Vpc::isDefault).findFirst().orElse(null);
    }

    public ContainerHealth verifySecurityGroupIds (String instanceId, List<String> originalSecurityGroupIds) {
        List<String> appliedSecurityGroups = getSecurityGroupIds(instanceId);
        return (originalSecurityGroupIds.containsAll(appliedSecurityGroups) && appliedSecurityGroups.containsAll(originalSecurityGroupIds)) ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK;
    }

    public List<String> getSecurityGroupIds (String instanceId) {
        return amazonEC2.describeInstanceAttribute(new DescribeInstanceAttributeRequest(instanceId, InstanceAttributeName.GroupSet))
                        .getInstanceAttribute()
                        .getGroups()
                        .stream()
                        .map(GroupIdentifier::getGroupId)
                        .collect(Collectors.toList());
    }
}
