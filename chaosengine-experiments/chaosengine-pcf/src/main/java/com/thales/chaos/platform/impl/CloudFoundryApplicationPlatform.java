/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.platform.impl;

import com.thales.chaos.constants.CloudFoundryConstants;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.CloudFoundryApplicationRouteType;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.CloudFoundryApplication;
import com.thales.chaos.container.impl.CloudFoundryApplicationRoute;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import com.thales.chaos.platform.enums.CloudFoundryIgnoredClientExceptions;
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

import static com.thales.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;
import static com.thales.chaos.container.enums.CloudFoundryApplicationRouteType.UNKNOWN;
import static com.thales.chaos.container.enums.CloudFoundryApplicationRouteType.mapFromString;
import static com.thales.chaos.exception.enums.CloudFoundryChaosErrorCode.EMPTY_RESPONSE;
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
                                                                  .filter(app -> app.getRequestedState().equals(CLOUDFOUNDRY_APPLICATION_STARTED))
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
            log.debug("Created Cloud Foundry Application Container {} from {}", v(DataDogConstants.DATADOG_CONTAINER_KEY, container), kv("ApplicationSummary", applicationSummary));
            containerManager.offer(container);
        } else {
            log.debug("Found existing Cloud Foundry Application Container {}", v(DataDogConstants.DATADOG_CONTAINER_KEY, container));
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
                Domain domain = getAppRouteDomain(routeEntity.getDomainId());
                if (domain != null) {
                    CloudFoundryApplicationRouteType cloudFoundryApplicationRouteType = mapFromString(domain.getType());
                    if (cloudFoundryApplicationRouteType != UNKNOWN) {
                        CloudFoundryApplicationRoute cloudFoundryApplicationRoute = CloudFoundryApplicationRoute.builder()
                                                                                                                .applicationName(applicationName)
                                                                                                                .route(routeEntity)
                                                                                                                .domain(domain)
                                                                                                                .build();
                        routes.add(cloudFoundryApplicationRoute);
                    } else {
                        log.warn("Application route {} will be skipped because it has unsupported type {}", domain.getName(), domain
                                .getType());
                    }
                }
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
        RestartApplicationRequest restartApplicationRequest = RestartApplicationRequest.builder().name(applicationName).build();
        cloudFoundryOperations.applications().restart(restartApplicationRequest).block();
    }

    public void unmapRoute (UnmapRouteRequest unmapRouteRequest) {
        cloudFoundryOperations.routes().unmap(unmapRouteRequest).block();
    }

    public void mapRoute (MapRouteRequest mapRouteRequest) {
        cloudFoundryOperations.routes().map(mapRouteRequest).block();
    }
}
