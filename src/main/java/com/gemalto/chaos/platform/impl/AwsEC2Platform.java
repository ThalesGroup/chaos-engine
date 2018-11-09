package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.constants.DataDogConstants;
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gemalto.chaos.constants.AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty("aws.ec2")
@ConfigurationProperties("aws.ec2")
public class AwsEC2Platform extends Platform {
    private AmazonEC2 amazonEC2;
    private ContainerManager containerManager;
    private Map<String, String> filter = new HashMap<>();
    private AwsEC2SelfAwareness awsEC2SelfAwareness;
    private String chaosSecurityGroupId;
    private Vpc defaultVpc;
    private List<String> groupingTags;

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

    @Override
    public List<Container> generateExperimentRoster () {
        List<Container> roster = getRoster();
        Map<String, List<AwsEC2Container>> groupedRoster = roster.stream()
                                                                 .map(AwsEC2Container.class::cast)
                                                                 .collect(Collectors.groupingBy(AwsEC2Container::getGroupIdentifier));
        List<Container> eligibleExperimentTargets = new ArrayList<>();
        groupedRoster.forEach((k, v) -> {
            // Anything that isn't in a managed group is eligible for experiments
            if (k.equals(AwsEC2Constants.NO_GROUPING_IDENTIFIER)) {
                eligibleExperimentTargets.addAll(v);
                return;
            }
            // If you set up an autoscaling group are scaled down to 1, you don't get a designated survivor.
            if (v.size() < 2) {
                log.warn("A scaled group contains less than 2 members. All will be eligible for experiments. {}", v("containers", v));
                eligibleExperimentTargets.addAll(v);
                return;
            }
            int designatedSurvivorIndex = new Random().nextInt(v.size());
            AwsEC2Container survivor = v.remove(designatedSurvivorIndex);
            log.debug("The container {} will be a designated survivor for this experiment", v(DataDogConstants.DATADOG_CONTAINER_KEY, survivor));
            eligibleExperimentTargets.addAll(v);
        });
        log.info("The following containers will be eligible for experiments: {}", v("experimentRoster", eligibleExperimentTargets));
        return eligibleExperimentTargets;
    }

    private AwsEC2Platform () {
        log.info("AWS EC2 Platform created");
    }

    public void setGroupingTags (List<String> groupingTags) {
        log.info("EC2 Instances will consider designated survivors based on the following tags: {}", v("groupingIdentifiers", groupingTags));
        this.groupingTags = groupingTags;
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
            containerList.addAll(describeInstancesResult.getReservations()
                                                        .stream()
                                                        .map(Reservation::getInstances)
                                                        .flatMap(Collection::parallelStream)
                                                        .filter(instance -> !awsEC2SelfAwareness.isMe(instance.getInstanceId()))
                                                        .map(this::createContainerFromInstance)
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet()));
            describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
            if (describeInstancesRequest.getNextToken() == null) {
                done = true;
                // Loops until all pages of instances have been resolved
            }
        }
        if (containerList.isEmpty()) {
            log.warn("No matching EC2 instance found.");
        }
        return containerList;
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

    /**
     * Creates a Container object from an EC2 Instance and appends it to a provided list of containers.
     *
     * @param instance      An EC2 Instance object to have a container created.
     */
    AwsEC2Container createContainerFromInstance (Instance instance) {
        if (instance.getState().getCode() == AwsEC2Constants.AWS_TERMINATED_CODE) return null;
        AwsEC2Container container = containerManager.getMatchingContainer(AwsEC2Container.class, instance.getInstanceId());
        if (container == null) {
            container = buildContainerFromInstance(instance);
            log.info("Found new AWS EC2 Container {}", v(DATADOG_CONTAINER_KEY, container));
            containerManager.offer(container);
        } else {
            log.debug("Found existing AWS EC2 Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    /**
     * Creates a Container object given an EC2 Instance object.
     *
     * @param instance Instance to have the Container created from.
     * @return Container mapping to the instance.
     */
    AwsEC2Container buildContainerFromInstance (Instance instance) {
        String name = instance.getTags()
                              .stream()
                              .filter(tag -> tag.getKey().equals("Name"))
                              .findFirst()
                              .orElse(new Tag("Name", "no-name"))
                              .getValue();
        final String groupIdentifier = Optional.ofNullable(groupingTags == null ? null : instance.getTags()
                                                                                                 .stream()
                                                                                                 .filter(tag -> groupingTags
                                                                                                         .contains(tag.getKey()))
                                                                                                 .min(Comparator.comparingInt(tag -> groupingTags
                                                                                     .indexOf(tag.getKey())))
                                                                                                 .orElse(new Tag())
                                                                                                 .getValue())
                                               .orElse(AwsEC2Constants.NO_GROUPING_IDENTIFIER);
        return AwsEC2Container.builder().awsEC2Platform(this)
                              .instanceId(instance.getInstanceId())
                              .keyName(instance.getKeyName()).name(name).groupIdentifier(groupIdentifier)
                              .build();
    }

    public ContainerHealth checkHealth (String instanceId) {
        Instance instance;
        InstanceState state;
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult result = amazonEC2.describeInstances(request);
        try {
            instance = result.getReservations().get(0).getInstances().get(0);
            state = instance.getState();
            if (state.getCode() == 48) {
                log.info("Instance {} is terminated", v(DataDogConstants.EC2_INSTANCE, instance));
                return ContainerHealth.DOES_NOT_EXIST;
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            // If Index 0 in array doesn't exist, or we get an NPE, it's because the instance doesn't exist anymore.
            log.error("Instance {} doesn't seem to exist anymore", instanceId, e);
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return state.getCode() == AwsEC2Constants.AWS_RUNNING_CODE ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
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
        return (originalSecurityGroupIds.containsAll(appliedSecurityGroups) && appliedSecurityGroups.containsAll(originalSecurityGroupIds)) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
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
