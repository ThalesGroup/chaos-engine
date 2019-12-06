/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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

