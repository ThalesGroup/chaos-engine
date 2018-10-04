package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.impl.attacks.ForkBomb;
import com.gemalto.chaos.ssh.impl.attacks.RandomProcessTermination;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class CloudFoundryContainer extends Container {
    private String applicationId;
    private String name;
    private Integer instance;
    private transient ArrayList<ShellSessionCapability> detectedCapabilities;
    private transient CloudFoundryContainerPlatform cloudFoundryContainerPlatform;
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryContainerPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };
    private transient Callable<Void> restartContainer = () -> {
        cloudFoundryContainerPlatform.restartInstance(getRestartApplicationInstanceRequest());
        return null;
    };
    private transient Callable<ContainerHealth> isInstanceRunning = () -> cloudFoundryContainerPlatform.checkHealth(applicationId, instance);

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
    public Platform getPlatform () {
        return cloudFoundryContainerPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return cloudFoundryContainerPlatform.checkHealth(applicationId, instance);
    }

    @Override
    public String getSimpleName () {
        return name + " - (" + instance + ")";
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withValue(name + "-" + instance);
    }

    @StateAttack
    public void restartContainer (Attack attack) {
        attack.setSelfHealingMethod(restageApplication);
        attack.setCheckContainerHealth(isInstanceRunning);
        cloudFoundryContainerPlatform.restartInstance(getRestartApplicationInstanceRequest());
    }

    private RestartApplicationInstanceRequest getRestartApplicationInstanceRequest () {
        RestartApplicationInstanceRequest restartApplicationInstanceRequest = RestartApplicationInstanceRequest.builder()
                                                                                                               .name(name)
                                                                                                               .instanceIndex(instance)
                                                                                                               .build();
        log.info("{}", restartApplicationInstanceRequest);
        return restartApplicationInstanceRequest;
    }

    @StateAttack
    public void forkBomb (Attack attack) {
        attack.setSelfHealingMethod(restartContainer);
        attack.setCheckContainerHealth(isInstanceRunning); // TODO Real healthcheck
        cloudFoundryContainerPlatform.sshAttack(new ForkBomb(), this);
    }

    @StateAttack
    public void terminateProcess (Attack attack) {
        attack.setSelfHealingMethod(restartContainer);
        attack.setCheckContainerHealth(isInstanceRunning); // TODO Real healtcheck
        cloudFoundryContainerPlatform.sshAttack(new RandomProcessTermination(), this);
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

    public ArrayList<ShellSessionCapability> getDetectedCapabilities () {
        return detectedCapabilities;
    }

    public void setDetectedCapabilities (ArrayList<ShellSessionCapability> detectedCapabilities) {
        this.detectedCapabilities = detectedCapabilities;
    }

    public static final class CloudFoundryContainerBuilder {
        private String applicationId;
        private String name;
        private Integer instance;
        private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;

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

        public CloudFoundryContainerBuilder platform (CloudFoundryContainerPlatform cloudFoundryContainerPlatform) {
            this.cloudFoundryContainerPlatform = cloudFoundryContainerPlatform;
            return this;
        }

        public CloudFoundryContainer build () {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            cloudFoundryContainer.cloudFoundryContainerPlatform = this.cloudFoundryContainerPlatform;
            return cloudFoundryContainer;
        }
    }
}
