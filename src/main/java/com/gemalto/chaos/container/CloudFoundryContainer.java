package com.gemalto.chaos.container;

public class CloudFoundryContainer implements Container {

    private String applicationId;
    private String name;

    private CloudFoundryContainer() {
    }

    @Override
    public String toString() {
        return name + ": " + applicationId;
    }

    public static final class CloudFoundryContainerBuilder {
        private String applicationid;
        private String name;

        private CloudFoundryContainerBuilder() {
        }

        public static CloudFoundryContainerBuilder aCloudFoundryContainer() {
            return new CloudFoundryContainerBuilder();
        }

        public CloudFoundryContainerBuilder withApplicationId(String applicationid) {
            this.applicationid = applicationid;
            return this;
        }

        public CloudFoundryContainerBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.applicationId = this.applicationid;
            cloudFoundryContainer.name = this.name;
            return cloudFoundryContainer;
        }
    }
}
