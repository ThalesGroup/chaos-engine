package com.gemalto.chaos.container;

import org.cloudfoundry.operations.applications.TerminateApplicationTaskRequest;

public class CloudFoundryContainer implements Container {

    private String applicationId;
    private String name;
    private Integer instances;


    public TerminateApplicationTaskRequest getTerminateApplicationTaskRequest() {
        return TerminateApplicationTaskRequest.builder()
                .applicationName(name)
                .sequenceId(instances)
                .build();
    }

    public static CloudFoundryContainerBuilder builder() {
        return CloudFoundryContainerBuilder.Builder();
    }

    private CloudFoundryContainer() {
    }

    @Override
    public String toString() {
        return name + ": " + applicationId;
    }

    public static final class CloudFoundryContainerBuilder {
        private String applicationId;
        private String name;
        private Integer instances;

        private CloudFoundryContainerBuilder() {
        }

        private static CloudFoundryContainerBuilder Builder() {
            return new CloudFoundryContainerBuilder();
        }

        public CloudFoundryContainerBuilder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public CloudFoundryContainerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CloudFoundryContainerBuilder instances(Integer instances) {
            this.instances = instances;
            return this;
        }

        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.instances = this.instances;
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.applicationId = this.applicationId;
            return cloudFoundryContainer;
        }
    }
}
