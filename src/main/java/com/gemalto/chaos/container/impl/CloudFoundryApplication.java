package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.ResourceAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryApplication extends Container {
    private transient static final Integer MAX_INSTANCES = 5;
    private transient static final Integer MIN_INSTANCES = 0;
    private transient static final Logger log = LoggerFactory.getLogger(CloudFoundryApplication.class);
    private String name;
    private Integer originalContainerInstances;
    private Integer actualContainerInstances;
    private transient CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    private transient Callable<Void> rescaleApplication = () -> {
        cloudFoundryApplicationPlatform.rescaleApplication(name, originalContainerInstances);
        return null;
    };

    public CloudFoundryApplication () {
        super();
    }

    public static CloudFoundryApplicationBuilder builder () {
        return CloudFoundryApplicationBuilder.builder();
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    @Override
    protected Platform getPlatform () {
        return cloudFoundryApplicationPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return cloudFoundryApplicationPlatform.checkApplicationHealth(name);
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

    @ResourceAttack
    public Callable<Void> scaleApplicationToMaxInstances () {
        return scaleApplication(MAX_INSTANCES);
    }

    private Callable<Void> scaleApplication (int instances) {
        actualContainerInstances = instances;
        log.debug("Scaling {} to {} instances", name);
        cloudFoundryApplicationPlatform.rescaleApplication(name, actualContainerInstances);
        return rescaleApplication;
    }

    @ResourceAttack
    public Callable<Void> scaleApplicationToMinInstances () {
        return scaleApplication(MIN_INSTANCES);
    }


    public static final class CloudFoundryApplicationBuilder {
        private String name;
        private Integer containerInstances;
        private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;

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

        public CloudFoundryApplicationBuilder platform (CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform) {
            this.cloudFoundryApplicationPlatform = cloudFoundryApplicationPlatform;
            return this;
        }

        public CloudFoundryApplication build () {
            CloudFoundryApplication cloudFoundryApplication = new CloudFoundryApplication();
            cloudFoundryApplication.name = this.name;
            cloudFoundryApplication.originalContainerInstances = this.containerInstances;
            cloudFoundryApplication.actualContainerInstances = this.containerInstances;
            cloudFoundryApplication.cloudFoundryApplicationPlatform = this.cloudFoundryApplicationPlatform;
            return cloudFoundryApplication;
        }
    }
}
