package com.gemalto.chaos.container;

import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;

public class CloudFoundryContainer implements Container {

    private String applicationId;
    private String name;
    private Integer instance;
    private Integer maxInstances;

    public RestartApplicationInstanceRequest getRestartApplicationInstanceRequest() {
        return RestartApplicationInstanceRequest.builder()
                .name(name)
                .instanceIndex(instance)
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
        private Integer maxInstances;

        private CloudFoundryContainerBuilder() {
        }

        static CloudFoundryContainerBuilder builder() {
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

        public CloudFoundryContainerBuilder maxInstances(Integer maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.maxInstances = this.maxInstances;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            return cloudFoundryContainer;
        }
    }
}
