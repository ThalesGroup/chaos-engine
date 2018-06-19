package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.CloudFoundryAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

public class CloudFoundryContainer extends Container {

    String applicationId;
    String name;
    Integer instance;
    CloudFoundryPlatform cloudFoundryPlatform;

    @Autowired
    private CloudFoundryContainer() {
        supportedAttackTypes.addAll(
                Arrays.asList(AttackType.STATE)
        );
    }

    public CloudFoundryContainer(String applicationId, String name, Integer instance) {
        this.applicationId = applicationId;
        this.name = name;
        this.instance = instance;
    }

    public static CloudFoundryContainerBuilder builder() {
        return CloudFoundryContainerBuilder.builder();
    }

    @Override
    protected Platform getPlatform() {
        return cloudFoundryPlatform;
    }

    @Override
    public Attack createAttack(AttackType attackType) {
        return CloudFoundryAttack.builder()
                .container(this)
                .attackType(attackType)
                .build();
    }

    @Override
    public void attackContainerState() {
        getPlatform().destroy(this);
    }

    @Override
    public void attackContainerResources() {
        getPlatform().degrade(this);

    }

    @Override
    public void attackContainerNetwork() {
        throw new UnsupportedOperationException();
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
        private CloudFoundryPlatform cloudFoundryPlatform;
        private FateManager fateManager;

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

        public CloudFoundryContainerBuilder platform(CloudFoundryPlatform cloudFoundryPlatform) {
            this.cloudFoundryPlatform = cloudFoundryPlatform;
            return this;
        }

        public CloudFoundryContainerBuilder fateManager(FateManager fateManager) {
            this.fateManager = fateManager;
            return this;
        }



        public CloudFoundryContainer build() {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            cloudFoundryContainer.cloudFoundryPlatform = this.cloudFoundryPlatform;
            cloudFoundryContainer.fateManager = this.fateManager;
            return cloudFoundryContainer;
        }
    }

}
