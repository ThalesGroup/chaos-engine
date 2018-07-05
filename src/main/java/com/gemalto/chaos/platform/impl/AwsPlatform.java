package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty({ "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY" })
public class AwsPlatform implements Platform {
    private static final Logger log = LoggerFactory.getLogger(AwsPlatform.class);
    @Autowired
    private AmazonEC2 amazonEC2;
    @Autowired
    private FateManager fateManager;
    private Map<String, String> filter = new HashMap<>();

    AwsPlatform (String[] filterKeys, String[] filterValues, AmazonEC2 amazonEC2, FateManager fateManager) {
        this(filterKeys, filterValues);
        this.amazonEC2 = amazonEC2;
        this.fateManager = fateManager;
    }

    @Autowired
    AwsPlatform (@Value("${AWS_FILTER_KEYS:#{null}}") String[] filterKeys, @Value("${AWS_FILTER_VALUES:#{null}}") String[] filterValues) {
        this();
        if (filterKeys != null && filterValues != null) {
            if (filterKeys.length != filterValues.length) {
                throw new ChaosException("Cannot start with an unequal amount of Filter Keys and Values");
            }
            for (int i = 0; i < filterKeys.length; i++) {
                filter.putIfAbsent(filterKeys[i], filterValues[i]);
            }
        }
    }

    private AwsPlatform () {
        log.info("AWS Platform created");
    }

    @Override
    public List<Container> getRoster () {
        return getRoster(filter);
    }

    private List<Container> getRoster (Map<String, String> filterValues) {
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
                    String name;
                    Optional<Tag> nameTag = instance.getTags()
                                                    .stream()
                                                    .filter(tag -> tag.getKey().equals("Name"))
                                                    .findFirst();
                    name = nameTag.isPresent() ? nameTag.get().getValue() : "no-name";
                    AwsEC2Container newContainer = AwsEC2Container.builder()
                                                                  .awsPlatform(this)
                                                                  .instanceId(instance.getInstanceId())
                                                                  .keyName(instance.getKeyName())
                                                                  .fateManager(fateManager)
                                                                  .name(name)
                                                                  .build();
                    // TODO : Implement use of ContainerManager to prevent creating duplicates (like PCF platform)
                    containerList.add(newContainer);
                    log.debug("{}", newContainer);
                }
            }
            describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
            if (describeInstancesRequest.getNextToken() == null) {
                done = true;
            }
        }
        return containerList;
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            amazonEC2.describeInstances();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            return ApiStatus.ERROR;
        }
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

    public void terminateInstance (String... instanceIds) {
        log.info("Requesting a Terminate of instances {}", (Object[]) instanceIds);
        amazonEC2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
    }

    public void startInstance (String... instanceIds) {
        log.info("Requesting a start of instances {}", (Object[]) instanceIds);
        amazonEC2.startInstances(new StartInstancesRequest().withInstanceIds(instanceIds));
    }
}
