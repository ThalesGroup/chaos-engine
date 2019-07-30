package com.thales.chaos.container.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.CloudFoundryChaosErrorCode;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryApplication extends Container {
    private static final Random RANDOM = new Random();
    private static final Integer MAX_INSTANCES = 5;
    private static final Integer MIN_INSTANCES = 1;
    @Identifier(order = 0)
    private String name;
    @Identifier(order = 1)
    private Integer originalContainerInstances;
    @Identifier(order = 2)
    private Integer actualContainerInstances;
    private String applicationID;
    private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    private List<CloudFoundryApplicationRoute> applicationRoutes;
    private CloudFoundryApplicationRoute routeUnderExperiment;
    private Runnable rescaleApplicationToDefault = () -> {
        cloudFoundryApplicationPlatform.rescaleApplication(name, originalContainerInstances);
        actualContainerInstances = originalContainerInstances;
    };
    private Runnable restageApplication = () -> cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
    private Runnable mapApplicationRoute = () -> {
        if (routeUnderExperiment != null) {
            log.debug("Mapping application route: {}", routeUnderExperiment);
            cloudFoundryApplicationPlatform.mapRoute(routeUnderExperiment.getMapRouteRequest());
        }
    };
    private Runnable noRecovery = () -> log.warn("There is no recovery method for this kind of experiment.");

    public List<CloudFoundryApplicationRoute> getApplicationRoutes () {
        return applicationRoutes;
    }

    private Callable<ContainerHealth> isAppHealthy = () -> cloudFoundryApplicationPlatform.checkPlatformHealth();

    public CloudFoundryApplication () {
        super();
    }

    public static CloudFoundryApplicationBuilder builder () {
        return CloudFoundryApplicationBuilder.builder();
    }

    public Integer getOriginalContainerInstances () {
        return originalContainerInstances;
    }

    public Integer getActualContainerInstances () {
        return actualContainerInstances;
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
        return getAggregationIdentifier();
    }

    @Override
    public String getAggregationIdentifier () {
        return name;
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey("application").withValue(name);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(name);
    }

    @ChaosExperiment(experimentType = ExperimentType.RESOURCE)
    public void scaleApplication (Experiment experiment) {
        experiment.setSelfHealingMethod(rescaleApplicationToDefault);
        experiment.setCheckContainerHealth(isAppHealthy);
        experiment.setFinalizeMethod(rescaleApplicationToDefault);
        int newScale;
        do {
            newScale = RANDOM.nextInt(MAX_INSTANCES - MIN_INSTANCES) + MIN_INSTANCES;
        } while (newScale == actualContainerInstances);
        actualContainerInstances = newScale;
        log.debug("Scaling {} to {} instances", name, actualContainerInstances);
        cloudFoundryApplicationPlatform.rescaleApplication(name, actualContainerInstances);
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void restartApplication (Experiment experiment) {
        experiment.setSelfHealingMethod(restageApplication);
        experiment.setCheckContainerHealth(isAppHealthy);
        cloudFoundryApplicationPlatform.restartApplication(name);
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void restageApplication (Experiment experiment) {
        experiment.setCheckContainerHealth(isAppHealthy);
        experiment.setSelfHealingMethod(noRecovery);
        cloudFoundryApplicationPlatform.restageApplication(getRestageApplicationRequest());
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    @ChaosExperiment(experimentType = ExperimentType.NETWORK)
    public void unmapRoute (Experiment experiment) {
        experiment.setCheckContainerHealth(isAppHealthy);
        if (!applicationRoutes.isEmpty()) {
            int routeIndex = RANDOM.nextInt(applicationRoutes.size());
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
