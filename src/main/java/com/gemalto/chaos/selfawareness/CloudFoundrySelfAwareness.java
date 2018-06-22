package com.gemalto.chaos.selfawareness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("CF_INSTANCE_GUID")
public class CloudFoundrySelfAwareness {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundrySelfAwareness.class);
    private String applicationId;
    private Integer applicationInstanceIndex;

    @Autowired
    CloudFoundrySelfAwareness (@Value("${vcap.application.id}") String applicationId, @Value("${CF_INSTANCE_INDEX}") Integer applicationInstanceIndex) {
        this.applicationId = applicationId;
        this.applicationInstanceIndex = applicationInstanceIndex;
        log.info("Detected running in Cloud Foundry");
    }

    public boolean isMe (String applicationId, Integer applicationInstanceIndex) {
        return applicationId.equals(this.applicationId) && applicationInstanceIndex.equals(this.applicationInstanceIndex);
    }
}
