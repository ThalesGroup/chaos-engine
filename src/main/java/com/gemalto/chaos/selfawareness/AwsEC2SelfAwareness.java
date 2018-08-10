package com.gemalto.chaos.selfawareness;

import com.gemalto.chaos.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Properties;

@Component
public class AwsEC2SelfAwareness {
    private static final Logger log = LoggerFactory.getLogger(AwsEC2SelfAwareness.class);
    private String instanceId;
    private boolean initialized = false;

    public boolean isMe (@NotNull String otherInstanceId) {
        if (!initialized) init();
        return (otherInstanceId.equals(instanceId));
    }

    private synchronized void init () {
        if (initialized) return;
        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            log.error("Error loading application.properties");
            initialized = true;
            return;
        }
        instanceId = fetchInstanceId(prop);
        if (instanceId == null) {
            log.info("This does not appear to be running in AWS EC2");
        } else {
            log.info("Running in AWS EC2 as Instance: {}", instanceId);
        }
        initialized = true;
    }

    String fetchInstanceId (Properties prop) {
        return HttpUtils.curl(String.format("http://%s/latest/meta-data/instance-id", prop.getProperty("aws.callback.host")), true);
    }
}
