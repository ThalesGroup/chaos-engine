package com.gemalto.chaos.container;

import org.cloudfoundry.operations.applications.TerminateApplicationTaskRequest;

public class CloudFoundryContainer implements Container {

    private String applicationId;
    private String name;
    private Integer instance;


    public TerminateApplicationTaskRequest getTerminateApplicationTaskRequest() {
        return TerminateApplicationTaskRequest.builder()
                .applicationName(name)
                .sequenceId(instance)
                .build();
    }

    public static CloudFoundryContainerBuilder builder() {
        return CloudFoundryContainerBuilder.builder();
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
        private Integer instance;

        private CloudFoundryContainerBuilder() {
        }

        private static CloudFoundryContainerBuilder builder() {
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

        public CloudFoundryContainerBuilder instance(Integer instance) {
            this.instance = instance;
            return this;
        }

        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.applicationId = this.applicationId;
            return cloudFoundryContainer;
        }
    }
}
