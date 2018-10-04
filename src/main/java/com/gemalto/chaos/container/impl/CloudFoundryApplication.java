package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.NetworkAttack;
import com.gemalto.chaos.attack.annotations.ResourceAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import java.time.Duration;
import java.util.List;
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
    private transient List<CloudFoundryApplicationRoute> applicationRoutes;
    private transient CloudFoundryApplicationRoute routeUnderAttack;
    private transient Callable<Void> rescaleApplicationToDefault = () -> {
        cloudFoundryApplicationPlatform.rescaleApplication(name, originalContainerInstances);
        actualContainerInstances = originalContainerInstances;
        return null;
    };
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };
    private transient Callable<Void> mapApplicationRoute = () -> {
        if (routeUnderAttack != null) {
            log.debug("Mapping application route: {}", routeUnderAttack);
            cloudFoundryApplicationPlatform.mapRoute(routeUnderAttack.getMapRouteRequest());
        }
        return null;
    };

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey("application")
                .withValue(name);
    }

    public Integer getOriginalContainerInstances () {
        return originalContainerInstances;
    }

    public Integer getActualContainerInstances () {
        return actualContainerInstances;
    }

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
        attack.setSelfHealingMethod(rescaleApplicationToDefault);
        attack.setCheckContainerHealth(isAppHealthy);
        attack.setFinalizeMethod(rescaleApplicationToDefault);
        Random rand = new Random();
        int newScale;
        do {
            newScale = rand.nextInt(MAX_INSTANCES - MIN_INSTANCES) + MIN_INSTANCES;
        } while (newScale == actualContainerInstances);
        actualContainerInstances = newScale;
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

    @NetworkAttack
    public void unmapRoute (Attack attack) {
        attack.setCheckContainerHealth(isAppHealthy);
        if (!applicationRoutes.isEmpty()) {
            Random rand = new Random();
            int routeIndex = rand.nextInt(applicationRoutes.size());
            this.routeUnderAttack = applicationRoutes.get(routeIndex);
            attack.setSelfHealingMethod(mapApplicationRoute);
            attack.setFinalizeMethod(mapApplicationRoute);
            log.debug("Unmapping application route: {}", routeUnderAttack);
            cloudFoundryApplicationPlatform.unmapRoute(routeUnderAttack.getUnmapRouteRequest());
        } else {
            attack.setFinalizationDuration(Duration.ZERO);
            attack.setSelfHealingMethod(noRecovery);
            log.warn("Application {} has no routes set, skipping the attack {}", name, attack.getId());
        }
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
        private List<CloudFoundryApplicationRoute> applicationRoutes;

        private CloudFoundryApplicationBuilder () {
        }

        static CloudFoundryApplicationBuilder builder () {
            return new CloudFoundryApplicationBuilder();
        }

        public CloudFoundryApplicationBuilder applicationRoutes (List<CloudFoundryApplicationRoute> applicationRoutes) {
            this.applicationRoutes = applicationRoutes;
            return this;
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
            cloudFoundryApplication.applicationRoutes = this.applicationRoutes;
            return cloudFoundryApplication;
        }
    }
}
