package com.gemalto.chaos.selfawareness;

import com.amazonaws.services.ec2.model.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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
        try {
            URL url = new URL(String.format("http://%s/latest/meta-data/instance-id", System.getProperty("aws.callback.host")));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setRequestMethod("GET");
            instanceId = connection.getResponseMessage();
            log.info("This appears to be an AWS Instance with ID: {}", instanceId);
        } catch (IOException e) {
            log.info("This does not appear to be an EC2 instance");
        } finally {
            initialized = true;
        }
    }

    public Filter getRequestFilter () {
        if (!initialized) init();
        return instanceId == null ? null : new Filter().withName("instance-id").withValues(instanceId);
    }
}
