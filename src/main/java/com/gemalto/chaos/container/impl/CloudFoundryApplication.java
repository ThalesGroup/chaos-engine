package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryApplication extends Container {
    private String name;
    private Integer containerInstances;
    private transient CloudFoundryPlatform cloudFoundryPlatform;
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };

    public CloudFoundryApplication () {
        super();
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    @Override
    protected Platform getPlatform () {
        return cloudFoundryPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        //TBD Check to be implemented
        return ContainerHealth.NORMAL;
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
        return name;
    }

    @StateAttack
    public Callable<Void> scaleApplication () {
        cloudFoundryPlatform.rescaleApplication(name, 2);
        return restageApplication;
    }

    public static CloudFoundryApplicationBuilder builder () {
        return CloudFoundryApplicationBuilder.builder();
    }

    public static final class CloudFoundryApplicationBuilder {
        private String name;
        private Integer containerInstances;
        private CloudFoundryPlatform cloudFoundryPlatform;

        private CloudFoundryApplicationBuilder () {
        }

        static CloudFoundryApplicationBuilder builder () {
            return new CloudFoundryApplicationBuilder();
        }

        public CloudFoundryApplicationBuilder name (String name) {
            this.name = name;
            return this;
        }

        public CloudFoundryApplicationBuilder containerInstances (Integer containerInstances) {
            this.containerInstances = containerInstances;
            return this;
        }

        public CloudFoundryApplicationBuilder platform (CloudFoundryPlatform cloudFoundryPlatform) {
            this.cloudFoundryPlatform = cloudFoundryPlatform;
            return this;
        }

        public CloudFoundryApplication build () {
            CloudFoundryApplication cloudFoundryApplication = new CloudFoundryApplication();
            cloudFoundryApplication.name = this.name;
            cloudFoundryApplication.containerInstances = this.containerInstances;
            cloudFoundryApplication.cloudFoundryPlatform = this.cloudFoundryPlatform;
            return cloudFoundryApplication;
        }
    }
}
