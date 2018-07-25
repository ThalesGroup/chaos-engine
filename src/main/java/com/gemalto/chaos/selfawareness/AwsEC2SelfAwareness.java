package com.gemalto.chaos.selfawareness;

import com.amazonaws.services.ec2.model.Filter;
import com.gemalto.chaos.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class AwsEC2SelfAwareness {
    private static final Logger log = LoggerFactory.getLogger(AwsEC2SelfAwareness.class);
    private String instanceId;
    private boolean initialized = false;

    public boolean isMe (@NotNull String otherInstanceId) {
        if (!initialized) init();
        return (otherInstanceId.equals(instanceId));
    }

    private void init () {
        instanceId = HttpUtils.curl(String.format("http://%s/latest/meta-data/instance-id", System.getProperty("aws.callback.host")));
        if (instanceId == null) {
            log.info("This does not appear to be running in AWS EC2");
        } else {
            log.info("Running in AWS EC2 as Instance: {}", instanceId);
        }
        initialized = true;
    }

    public Filter getRequestFilter () {
        if (!initialized) init();
        return instanceId == null ? null : new Filter().withName("instance-id").withValues(instanceId);
    }
}
