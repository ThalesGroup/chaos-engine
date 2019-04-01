package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.exception.enums.CloudFoundryChaosErrorCode;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.ResourceExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private transient CloudFoundryApplicationRoute routeUnderExperiment;
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
        if (routeUnderExperiment != null) {
            log.debug("Mapping application route: {}", routeUnderExperiment);
            cloudFoundryApplicationPlatform.mapRoute(routeUnderExperiment.getMapRouteRequest());
        }
        return null;
    };

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey("application")
                .withValue(name);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(name);
    }

    public Integer getOriginalContainerInstances () {
        return originalContainerInstances;
    }

    public Integer getActualContainerInstances () {
        return actualContainerInstances;
    }

    private transient Callable<Void> noRecovery = () -> {
        log.warn("There is no recovery method for this kind of experiment.");
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
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return cloudFoundryApplicationPlatform.checkPlatformHealth();
    }

    @Override
    public String getSimpleName () {
        return name;
    }

    @ResourceExperiment
    public void scaleApplication (Experiment experiment) {
        experiment.setSelfHealingMethod(rescaleApplicationToDefault);
        experiment.setCheckContainerHealth(isAppHealthy);
        experiment.setFinalizeMethod(rescaleApplicationToDefault);
        Random rand = new Random();
        int newScale;
        do {
            newScale = rand.nextInt(MAX_INSTANCES - MIN_INSTANCES) + MIN_INSTANCES;
        } while (newScale == actualContainerInstances);
        actualContainerInstances = newScale;
        log.debug("Scaling {} to {} instances", name, actualContainerInstances);
        cloudFoundryApplicationPlatform.rescaleApplication(name, actualContainerInstances);
    }

    @StateExperiment
    public void restartApplication (Experiment experiment) {
        experiment.setSelfHealingMethod(restageApplication);
        experiment.setCheckContainerHealth(isAppHealthy);
        cloudFoundryApplicationPlatform.restartApplication(name);
    }

    @StateExperiment
    public void restageApplication (Experiment experiment) {
        experiment.setCheckContainerHealth(isAppHealthy);
        experiment.setSelfHealingMethod(noRecovery);
        cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
    }

    @NetworkExperiment
    public void unmapRoute (Experiment experiment) {
        experiment.setCheckContainerHealth(isAppHealthy);
        if (!applicationRoutes.isEmpty()) {
            Random rand = new Random();
            int routeIndex = rand.nextInt(applicationRoutes.size());
            this.routeUnderExperiment = applicationRoutes.get(routeIndex);
            experiment.setSelfHealingMethod(mapApplicationRoute);
            experiment.setFinalizeMethod(mapApplicationRoute);
            log.debug("Unmapping application route: {}", routeUnderExperiment);
            cloudFoundryApplicationPlatform.unmapRoute(routeUnderExperiment.getUnmapRouteRequest());
        } else {
            log.warn("Application {} has no routes set, stopping the experiment {}", name, experiment.getId());
            throw new ChaosException(CloudFoundryChaosErrorCode.NO_ROUTES);
        }
    }


    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    public static final class CloudFoundryApplicationBuilder {
        private final Map<String, String> dataDogTags = new HashMap<>();
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
            return dataDogTag("application_id", applicationID);
        }

        public CloudFoundryApplicationBuilder dataDogTag (String key, String value) {
            this.dataDogTags.put(key, value);
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

        public CloudFoundryApplicationBuilder name (String name) {
            this.name = name;
            return dataDogTag("application_name", name);
        }

        public CloudFoundryApplication build () {
            CloudFoundryApplication cloudFoundryApplication = new CloudFoundryApplication();
            cloudFoundryApplication.name = this.name;
            cloudFoundryApplication.originalContainerInstances = this.containerInstances;
            cloudFoundryApplication.actualContainerInstances = this.containerInstances;
            cloudFoundryApplication.cloudFoundryApplicationPlatform = this.cloudFoundryApplicationPlatform;
            cloudFoundryApplication.applicationID = this.applicationID;
            cloudFoundryApplication.applicationRoutes = this.applicationRoutes;
            cloudFoundryApplication.dataDogTags.putAll(this.dataDogTags);
            return cloudFoundryApplication;
        }
    }
}
