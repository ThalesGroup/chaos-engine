package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.CloudFoundryAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Random;

public class CloudFoundryContainer extends Container {
    private String applicationId;
    private String name;
    private Integer instance;
    private CloudFoundryPlatform cloudFoundryPlatform;

    @Autowired
    private CloudFoundryContainer () {
        supportedAttackTypes.addAll(Arrays.asList(AttackType.STATE));
    }

    CloudFoundryContainer (String applicationId, String name, Integer instance) {
        this.applicationId = applicationId;
        this.name = name;
        this.instance = instance;
    }

    public static CloudFoundryContainerBuilder builder () {
        return CloudFoundryContainerBuilder.builder();
    }

    @Override
    protected Platform getPlatform () {
        return cloudFoundryPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return cloudFoundryPlatform.checkHealth(applicationId, attackType);
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return CloudFoundryAttack.builder()
                                 .container(this)
                                 .attackType(attackType)
                                 .timeToLive(new Random().nextInt(5))
                                 .build();
    }

    @Override
    public void attackContainerState () {
        cloudFoundryPlatform.restartInstance(getRestartApplicationInstanceRequest());
    }

    @Override
    public void attackContainerNetwork () {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attackContainerResources () {
        cloudFoundryPlatform.degrade(this);
    }


    private RestartApplicationInstanceRequest getRestartApplicationInstanceRequest () {
        RestartApplicationInstanceRequest restartApplicationInstanceRequest = RestartApplicationInstanceRequest.builder()
                                                                                                               .name(name)
                                                                                                               .instanceIndex(instance)
                                                                                                               .build();
        log.info("{}", restartApplicationInstanceRequest);
        return restartApplicationInstanceRequest;
    }

    public String getApplicationId () {
        return applicationId;
    }

    public String getName () {
        return name;
    }

    public Integer getInstance () {
        return instance;
    }

    public static final class CloudFoundryContainerBuilder {
        private String applicationId;
        private String name;
        private Integer instance;
        private CloudFoundryPlatform cloudFoundryPlatform;
        private FateManager fateManager;

        private CloudFoundryContainerBuilder () {
        }

        static CloudFoundryContainerBuilder builder () {
            return new CloudFoundryContainerBuilder();
        }

        public CloudFoundryContainerBuilder applicationId (String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public CloudFoundryContainerBuilder name (String name) {
            this.name = name;
            return this;
        }

        public CloudFoundryContainerBuilder instance (Integer instance) {
            this.instance = instance;
            return this;
        }

        public CloudFoundryContainerBuilder platform (CloudFoundryPlatform cloudFoundryPlatform) {
            this.cloudFoundryPlatform = cloudFoundryPlatform;
            return this;
        }

        public CloudFoundryContainerBuilder fateManager (FateManager fateManager) {
            this.fateManager = fateManager;
            return this;
        }

        public CloudFoundryContainer build () {
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
