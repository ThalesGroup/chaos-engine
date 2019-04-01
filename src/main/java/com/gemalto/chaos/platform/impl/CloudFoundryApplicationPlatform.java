package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.constants.CloudFoundryConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryApplication;
import com.gemalto.chaos.container.impl.CloudFoundryApplicationRoute;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.exception.enums.ChaosErrorCode;
import com.gemalto.chaos.platform.enums.CloudFoundryIgnoredClientExceptions;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.routes.RouteResource;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static com.gemalto.chaos.exception.enums.CloudFoundryChaosErrorCode.EMPTY_RESPONSE;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty({ "cf.applicationChaos" })
@ConfigurationProperties("cf")
public class CloudFoundryApplicationPlatform extends CloudFoundryPlatform {
    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;
    @Autowired
    private CloudFoundryClient cloudFoundryClient;
    @Autowired
    private ContainerManager containerManager;

    @Autowired
    public CloudFoundryApplicationPlatform () {
        log.info("PCF Application Platform created");
    }

    public ContainerHealth checkPlatformHealth () {
        Iterable<ApplicationSummary> apps = cloudFoundryOperations.applications()
                                                                  .list()
                                                                  .filter(app -> app.getRequestedState()
                                                                                    .equals(CLOUDFOUNDRY_APPLICATION_STARTED))
                                                                  .filter(app -> app.getInstances() > 0)
                                                                  .toIterable();
        for (ApplicationSummary app : apps) {
            ContainerHealth appHealth = checkApplicationHealth(app.getName(), app.getId());
            if (ContainerHealth.NORMAL != appHealth) {
                return appHealth;
            }
        }
        return ContainerHealth.NORMAL;
    }

    private ContainerHealth checkApplicationHealth (String applicationName, String applicationID) {
        Map<String, ApplicationInstanceInfo> applicationInstanceResponse;
        try {
            applicationInstanceResponse = cloudFoundryClient.applicationsV2()
                                                            .instances(ApplicationInstancesRequest.builder()
                                                                                                  .applicationId(applicationID)
                                                                                                  .build())
                                                            .blockOptional()
                                                            .orElseThrow(EMPTY_RESPONSE.asChaosException())
                                                            .getInstances();
        } catch (ClientV2Exception e) {
            if (CloudFoundryIgnoredClientExceptions.isIgnorable(e)) {
                log.warn("Platform returned ignorable exception: {} ", e.getMessage(), e);
                return ContainerHealth.RUNNING_EXPERIMENT;
            }
            log.error("Cannot get application instances: {}", e.getMessage(), e);
            return ContainerHealth.DOES_NOT_EXIST;
        }
        String status;
        try {
            status = CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE;
            for (Map.Entry<String, ApplicationInstanceInfo> appInfo : applicationInstanceResponse.entrySet()) {
                if (!CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE.equals(appInfo.getValue().getState())) {
                    status = appInfo.getValue().getState();
                    log.warn("Application {} is not healthy. Actual state is {}", applicationName, status);
                    break;
                }
            }
        } catch (NullPointerException e) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return (status.equals(CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
    }

    @Override
    public List<Container> generateRoster () {
        return cloudFoundryOperations.applications()
                                     .list()
                                     .toStream()
                                     .filter(app -> app.getRequestedState().equals(CLOUDFOUNDRY_APPLICATION_STARTED))
                                     .filter(applicationSummary -> {
                                         Integer instances = applicationSummary.getInstances();
                                         if (instances <= 0) {
                                             log.debug("Skipping {} which has {} container instances.", applicationSummary
                                                     .getName(), instances);
                                             return false;
                                         }
                                         return true;
                                     })
                                     .filter(applicationSummary -> {
                                         if (this.isChaosEngine(applicationSummary.getName())) {
                                             log.debug("Skipping {} what appears to be me.", applicationSummary.getName());
                                             return false;
                                         }
                                         return true;
                                     })
                                     .map(this::createApplicationFromApplicationSummary)
                                     .filter(Objects::nonNull)
                                     .distinct()
                                     .collect(Collectors.toList());
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        throw new ChaosException(ChaosErrorCode.PLATFORM_DOES_NOT_SUPPORT_RECYCLING);
    }

    CloudFoundryApplication createApplicationFromApplicationSummary (ApplicationSummary applicationSummary) {
        CloudFoundryApplication container = containerManager.getMatchingContainer(CloudFoundryApplication.class, applicationSummary
                .getName());
        if (container == null) {
            container = CloudFoundryApplication.builder()
                                               .platform(this)
                                               .name(applicationSummary.getName())
                                               .applicationID(applicationSummary.getId())
                                               .containerInstances(applicationSummary.getInstances())
                                               .applicationRoutes(gatherApplicationRoutes(applicationSummary.getName(), applicationSummary
                                                       .getId()))
                                               .build();
            log.debug("Created Cloud Foundry Application Container {} from {}", v(DATADOG_CONTAINER_KEY, container), kv("ApplicationSummary", applicationSummary));
            containerManager.offer(container);
        } else {
            log.debug("Found existing Cloud Foundry Application Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    List<CloudFoundryApplicationRoute> gatherApplicationRoutes (String applicationName, String applicationId) {
        List<CloudFoundryApplicationRoute> routes = new ArrayList<>();
        ListApplicationRoutesRequest listApplicationRoutesRequest = ListApplicationRoutesRequest.builder()
                                                                                                .applicationId(applicationId)
                                                                                                .build();
        ListApplicationRoutesResponse listApplicationRoutesResponse = cloudFoundryClient.applicationsV2()
                                                                                        .listRoutes(listApplicationRoutesRequest)
                                                                                        .blockOptional()
                                                                                        .orElseThrow(EMPTY_RESPONSE.asChaosException());
        List<RouteResource> routeResources = listApplicationRoutesResponse.getResources();
        if (routeResources != null) {
            for (RouteResource route : routeResources) {
                RouteEntity routeEntity = route.getEntity();
                CloudFoundryApplicationRoute cloudFoundryApplicationRoute = CloudFoundryApplicationRoute.builder()
                                                                                                        .applicationName(applicationName)
                                                                                                        .route(routeEntity)
                                                                                                        .domain(getAppRouteDomain(routeEntity
                                                                                                                .getDomainId()))
                                                                                                        .build();
                routes.add(cloudFoundryApplicationRoute);
            }
        }

        log.debug("Application {} routes: {}", applicationName, routes);
        return routes;
    }

    private Domain getAppRouteDomain (String domainID) {
        Flux<Domain> domains = cloudFoundryOperations.domains().list();
        for (Domain domain : domains.toIterable()) {
            if (domainID.equals(domain.getId())) {
                return domain;
            }
        }
        return null;
    }
    public void rescaleApplication (String applicationName, int instances) {
        ScaleApplicationRequest scaleApplicationRequest = ScaleApplicationRequest.builder()
                                                                                 .name(applicationName)
                                                                                 .instances(instances)
                                                                                 .build();
        cloudFoundryOperations.applications().scale(scaleApplicationRequest).block();
    }

    public void restartApplication (String applicationName) {
        RestartApplicationRequest restartApplicationRequest = RestartApplicationRequest.builder()
                                                                                       .name(applicationName)
                                                                                       .build();
        cloudFoundryOperations.applications().restart(restartApplicationRequest).block();
    }

    public void unmapRoute (UnmapRouteRequest unmapRouteRequest) {
        cloudFoundryOperations.routes().unmap(unmapRouteRequest).block();
    }

    public void mapRoute (MapRouteRequest mapRouteRequest) {
        cloudFoundryOperations.routes().map(mapRouteRequest).block();
    }
}
