package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.CloudFoundryAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;

import java.util.Arrays;

public class CloudFoundryContainer extends Container {

    private String applicationId;
    private String name;
    private Integer instance;
    private Integer maxInstances;

    private CloudFoundryContainer() {
        supportedAttackTypes.addAll(
                Arrays.asList(AttackType.STATE)
        );
    }

    public static CloudFoundryContainerBuilder builder() {
        return CloudFoundryContainerBuilder.builder();
    }

    @Override
    protected void updateContainerHealth() {
        // TODO : Actually calculate Container Health
        containerHealth = ContainerHealth.NORMAL;
    }

    @Override
    public Attack createAttack(AttackType attackType) {
        return CloudFoundryAttack.builder()
                .container(this)
                .attackType(attackType)
                .build();
    }

    public RestartApplicationInstanceRequest getRestartApplicationInstanceRequest() {
        return RestartApplicationInstanceRequest.builder()
                .name(name)
                .instanceIndex(instance)
                .build();
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
