package com.gemalto.chaos.selfawareness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty("CF_INSTANCE_GUID")
public class CloudFoundrySelfAwareness {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundrySelfAwareness.class);
    private String applicationName;
    private Integer applicationInstanceIndex;
    private List<String> linkedApplicationNames;

    @Autowired
    CloudFoundrySelfAwareness (@Value("${vcap.application.name}") String applicationName, @Value("${CF_INSTANCE_INDEX}") Integer applicationInstanceIndex, @Value("${cf_linked_applications}") String[] linkedApplicationNames) {
        log.info("Detected running in Cloud Foundry");
        log.info("Application name: {}", applicationName);
        log.info("Application Index: {}", applicationInstanceIndex);
        this.applicationName = applicationName;
        this.applicationInstanceIndex = applicationInstanceIndex;
        this.linkedApplicationNames = Arrays.asList(linkedApplicationNames);
    }

    public boolean isMe (String applicationName, Integer applicationInstanceIndex) {
        return (applicationName.equals(this.applicationName) && applicationInstanceIndex.equals(this.applicationInstanceIndex));
    }

    public boolean isFriendly (String applicationName) {
        return (linkedApplicationNames.contains(applicationName));
    }
}
