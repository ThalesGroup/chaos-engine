package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.ResourceAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryApplication extends Container {
    private transient static final Integer MAX_INSTANCES = 5;
    private transient static final Integer MIN_INSTANCES = 1;
    private String name;
    private Integer originalContainerInstances;
    private Integer actualContainerInstances;
    private transient String applicationID;
    private transient CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    private transient Callable<Void> rescaleApplication = () -> {
        cloudFoundryApplicationPlatform.rescaleApplication(name, originalContainerInstances);
        return null;
    };
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };
    private transient Callable<Void> noRecovery = () -> {
        log.warn("There is no recovery method for this kind of attack.");
        return null;
    };
    private transient Callable<ContainerHealth> isAppHealthy = () -> cloudFoundryApplicationPlatform.checkPlatformHealth();

    public CloudFoundryApplication () {
        super();
    }

    public static CloudFoundryApplicationBuilder builder () {
        return CloudFoundryApplicationBuilder.builder();
    }

    public String getApplicationID () {
        return applicationID;
    }

    @Override
    public Platform getPlatform () {
        return cloudFoundryApplicationPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return cloudFoundryApplicationPlatform.checkPlatformHealth();
    }

    @Override
    public String getSimpleName () {
        return name;
    }

    @ResourceAttack
    public void scaleApplication (Attack attack) {
        attack.setSelfHealingMethod(rescaleApplication);
        attack.setCheckContainerHealth(isAppHealthy);
        attack.setFinalizeMethod(rescaleApplication);
        Random rand = new Random();
        actualContainerInstances = rand.nextInt(MAX_INSTANCES - MIN_INSTANCES) + MIN_INSTANCES;
        log.debug("Scaling {} to {} instances", name, actualContainerInstances);
        cloudFoundryApplicationPlatform.rescaleApplication(name, actualContainerInstances);
    }

    @StateAttack
    public void restartApplication (Attack attack) {
        attack.setSelfHealingMethod(restageApplication);
        attack.setCheckContainerHealth(isAppHealthy);
        cloudFoundryApplicationPlatform.restartApplication(name);
    }

    @StateAttack
    public void restageApplication (Attack attack) {
        attack.setCheckContainerHealth(isAppHealthy);
        attack.setSelfHealingMethod(noRecovery);
        cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    public static final class CloudFoundryApplicationBuilder {
        private String name;
        private Integer containerInstances;
        private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
        private String applicationID;

        private CloudFoundryApplicationBuilder () {
        }

        static CloudFoundryApplicationBuilder builder () {
            return new CloudFoundryApplicationBuilder();
        }

        public CloudFoundryApplicationBuilder applicationID (String applicationID) {
            this.applicationID = applicationID;
            return this;
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
            cloudFoundryApplication.applicationID = this.applicationID;
            return cloudFoundryApplication;
        }
    }
}
