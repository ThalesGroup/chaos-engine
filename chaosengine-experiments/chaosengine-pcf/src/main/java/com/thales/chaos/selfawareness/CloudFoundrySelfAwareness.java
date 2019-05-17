package com.thales.chaos.selfawareness;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty("CF_INSTANCE_GUID")
public class CloudFoundrySelfAwareness {
    @Value("${vcap.application.name:@null}")
    private String applicationName;
    @Value(value = "${cf_linked_applications:@null}")
    private List<String> linkedApplicationNames;

    public static CloudFoundrySelfAwarenessBuilder builder () {
        return CloudFoundrySelfAwarenessBuilder.aCloudFoundrySelfAwareness();
    }

    public boolean isMe (String applicationName) {
        return applicationName != null && applicationName.equals(this.applicationName);
    }

    public boolean isFriendly (String applicationName) {
        return linkedApplicationNames != null && linkedApplicationNames.contains(applicationName);
    }

    public static final class CloudFoundrySelfAwarenessBuilder {
        private String applicationName;
        private List<String> linkedApplicationNames;

        private CloudFoundrySelfAwarenessBuilder () {
        }

        static CloudFoundrySelfAwarenessBuilder aCloudFoundrySelfAwareness () {
            return new CloudFoundrySelfAwarenessBuilder();
        }

        CloudFoundrySelfAwarenessBuilder withApplicationName (String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        CloudFoundrySelfAwarenessBuilder withLinkedApplicationNames (List<String> linkedApplicationNames) {
            this.linkedApplicationNames = linkedApplicationNames;
            return this;
        }

        public CloudFoundrySelfAwareness build () {
            CloudFoundrySelfAwareness cloudFoundrySelfAwareness = new CloudFoundrySelfAwareness();
            cloudFoundrySelfAwareness.linkedApplicationNames = this.linkedApplicationNames;
            cloudFoundrySelfAwareness.applicationName = this.applicationName;
            return cloudFoundrySelfAwareness;
        }
    }
}

