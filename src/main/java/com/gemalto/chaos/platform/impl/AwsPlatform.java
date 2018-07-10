package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty({ "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY" })
public class AwsPlatform extends Platform {
    private AmazonEC2 amazonEC2;
    private FateManager fateManager;
    private ContainerManager containerManager;
    private Map<String, String> filter = new HashMap<>();

    @Autowired
    AwsPlatform (@Value("${AWS_FILTER_KEYS:#{null}}") String[] filterKeys, @Value("${AWS_FILTER_VALUES:#{null}}") String[] filterValues, AmazonEC2 amazonEC2, FateManager fateManager, ContainerManager containerManager) {
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
        this.fateManager = fateManager;
        this.containerManager = containerManager;
    }

    private AwsPlatform () {
        log.info("AWS Platform created");
    }

    @Override
    public List<Container> generateRoster () {
        return generateRosterImpl(filter);
    }

    private List<Container> generateRosterImpl (Map<String, String> filterValues) {
        List<Container> containerList = new ArrayList<>();
        Collection<Filter> filters = new HashSet<>();
        if (filterValues != null) {
            filterValues.forEach((k, v) -> filters.add(new Filter().withName("tag:" + k).withValues(v)));
        }
        boolean done = false;
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withFilters(filters);
        while (!done) {
            DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
            for (Reservation reservation : describeInstancesResult.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    if (instance.getState().getCode() == AwsEC2Constants.AWS_TERMINATED_CODE) continue;
                    Container newContainer = createContainerFromInstance(instance);
                    Container container = containerManager.getOrCreatePersistentContainer(newContainer);
                    if (container != newContainer) {
                        log.debug("Found existing container {}", container);
                    } else {
                        log.info("Found new container {}", container);
                    }
                    containerList.add(container);
                }
            }
            describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
            if (describeInstancesRequest.getNextToken() == null) {
                done = true;
            }
        }
        return containerList;
    }

    private Container createContainerFromInstance (Instance instance) {
        String name;
        Optional<Tag> nameTag = instance.getTags().stream().filter(tag -> tag.getKey().equals("Name")).findFirst();
        name = nameTag.isPresent() ? nameTag.get().getValue() : "no-name";
        return AwsEC2Container.builder()
                              .awsPlatform(this)
                              .instanceId(instance.getInstanceId())
                              .keyName(instance.getKeyName())
                              .fateManager(fateManager)
                              .name(name)
                              .build();
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            amazonEC2.describeInstances();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            log.error("API for AWS failed to resolve.", e);
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
        Set<InstanceState> instanceStates = instances.map(Instance::getState).distinct().collect(Collectors.toSet());
        Set<Integer> instanceStateCodes = instanceStates.stream()
                                                        .map(InstanceState::getCode)
                                                        .collect(Collectors.toSet());
        for (int state : AwsEC2Constants.getAwsUnhealthyCodes()) {
            if (instanceStateCodes.contains(state)) return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
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
}
