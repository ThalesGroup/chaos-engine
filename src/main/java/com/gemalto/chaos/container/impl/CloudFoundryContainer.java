package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import com.gemalto.chaos.ssh.impl.attacks.ForkBomb;
import com.gemalto.chaos.ssh.impl.attacks.RandomProcessTermination;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;

import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryContainer extends Container {
    private String applicationId;
    private String name;
    private Integer instance;
    private transient CloudFoundryPlatform cloudFoundryPlatform;
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };
    private transient Callable<Void> restartContainer = () -> {
        cloudFoundryPlatform.restartInstance(getRestartApplicationInstanceRequest());
        return null;
    };
    private transient Callable<ContainerHealth> isInstanceRunning = () -> cloudFoundryPlatform.checkHealth(applicationId, instance);

    private CloudFoundryContainer () {
        super();
    }

    CloudFoundryContainer (String applicationId, String name, Integer instance) {
        super();
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
        return cloudFoundryPlatform.checkHealth(applicationId, instance);
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return GenericContainerAttack.builder()
                                     .withContainer(this)
                                     .withAttackType(attackType)
                                     .withTimeToLive(new Random().nextInt(5))
                                     .build();
    }

    @Override
    public String getSimpleName () {
        return name + " - (" + instance + ")";
    }

    @StateAttack
    public void restartContainer (Attack attack) {
        attack.setSelfHealingMethod(restageApplication);
        attack.setCheckContainerHealth(isInstanceRunning);
        cloudFoundryPlatform.restartInstance(getRestartApplicationInstanceRequest());
    }

    @StateAttack
    public void forkBomb (Attack attack) {
        attack.setSelfHealingMethod(restartContainer);
        attack.setCheckContainerHealth(isInstanceRunning); // TODO Real healthcheck
        cloudFoundryPlatform.sshAttack(new ForkBomb(), this);
    }

    @StateAttack
    public void terminateProcess (Attack attack) {
        attack.setSelfHealingMethod(restartContainer);
        attack.setCheckContainerHealth(isInstanceRunning); // TODO Real healtcheck
        cloudFoundryPlatform.sshAttack(new RandomProcessTermination(), this);
    }

    private RestartApplicationInstanceRequest getRestartApplicationInstanceRequest () {
        RestartApplicationInstanceRequest restartApplicationInstanceRequest = RestartApplicationInstanceRequest.builder()
                                                                                                               .name(name)
                                                                                                               .instanceIndex(instance)
                                                                                                               .build();
        log.info("{}", restartApplicationInstanceRequest);
        return restartApplicationInstanceRequest;
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
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

        public CloudFoundryContainer build () {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            cloudFoundryContainer.cloudFoundryPlatform = this.cloudFoundryPlatform;
            return cloudFoundryContainer;
        }
    }
}
