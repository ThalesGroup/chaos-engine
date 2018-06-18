package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.CloudFoundryAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

public class CloudFoundryContainer extends Container {

    private String applicationId;
    private String name;
    private Integer instance;

    @Autowired
    private CloudFoundryPlatform cloudFoundryPlatform;

    @Override
    protected Platform getPlatform() {
        return cloudFoundryPlatform;
    }

    private CloudFoundryContainer() {
        supportedAttackTypes.addAll(
                Arrays.asList(AttackType.STATE)
        );
    }

    public static CloudFoundryContainerBuilder builder() {
        return CloudFoundryContainerBuilder.builder();
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


    public static final class CloudFoundryContainerBuilder {
        private String applicationId;
        private String name;
        private Integer instance;

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

        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            return cloudFoundryContainer;
        }
    }

}
