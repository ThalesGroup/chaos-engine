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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.compute.v1.Items;
import com.google.cloud.redis.v1.*;
import com.google.longrunning.Operation;
import com.google.longrunning.OperationsClient;
import com.thales.chaos.constants.GcpConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpMemorystoreInstanceContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.services.impl.GcpCredentialsMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thales.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static com.thales.chaos.exception.enums.GcpMemorystoreChaosErrorCode.GCP_MEMORYSTORE_DOES_NOT_SUPPORT_RECYCLING;
import static com.thales.chaos.exception.enums.GcpMemorystoreChaosErrorCode.GCP_MEMORYSTORE_GENERIC_ERROR;
import static java.util.Collections.emptySet;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

@ConditionalOnProperty("gcp.memorystore")
@ConfigurationProperties("gcp.memorystore")
@Component
public class GcpMemorystorePlatform extends Platform {
    @Autowired
    private CredentialsProvider computeCredentialsProvider;
    @Autowired
    private ContainerManager containerManager;
    @Autowired
    private GcpCredentialsMetadata gcpCredentialsMetadata;
    private Map<String, String> includeFilter = Collections.emptyMap();
    private Map<String, String> excludeFilter = Collections.emptyMap();

    public GcpMemorystorePlatform () {
        log.info("GCP Memorystore Platform created");
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            LocationName parent = LocationName.of(gcpCredentialsMetadata.getProjectId(),
                    GcpConstants.MEMORYSTORE_LOCATION_WILDCARD);
            getInstances(parent);
        } catch (RuntimeException e) {
            log.error("Caught error when evaluating API Status of Google Cloud Platform", e);
            return ApiStatus.ERROR;
        }
        return ApiStatus.OK;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        try {
            LocationName parent = LocationName.of(gcpCredentialsMetadata.getProjectId(),
                    GcpConstants.MEMORYSTORE_LOCATION_WILDCARD);
            return getInstances(parent).iterator().hasNext() ? PlatformHealth.OK : PlatformHealth.DEGRADED;
        } catch (RuntimeException e) {
            log.error("Memorystore Platform health check failed", e);
            return PlatformHealth.FAILED;
        }
    }

    @Override
    protected List<Container> generateRoster () {
        log.debug("Generating roster of GCP Memorystore instances");
        LocationName parent = LocationName.of(gcpCredentialsMetadata.getProjectId(),
                GcpConstants.MEMORYSTORE_LOCATION_WILDCARD);
        Iterable<Instance> instanceIterable = getInstances(parent);
        return StreamSupport.stream(instanceIterable.spliterator(), false)
                            .filter(Objects::nonNull)
                            .filter(this::isNotFiltered)
                            .filter(this::isReady)
                            .filter(this::isHA)
                            .map(this::createContainerFromInstance)
                            .peek(container -> log.info("Created container {}", v(DATADOG_CONTAINER_KEY, container)))
                            .collect(Collectors.toList());
    }

    private static Collection<Items> asItemCollection (Map<String, String> itemMap) {
        return itemMap.entrySet()
                      .stream()
                      .map(entrySet -> Items.newBuilder()
                                            .setKey(entrySet.getKey())
                                            .setValue(entrySet.getValue())
                                            .build())
                      .collect(Collectors.toSet());
    }

    public Collection<Items> getIncludeFilter () {
        return asItemCollection(includeFilter);
    }

    public void setIncludeFilter (Map<String, String> includeFilter) {
        this.includeFilter = includeFilter;
    }

    public Collection<Items> getExcludeFilter () {
        return asItemCollection(excludeFilter);
    }

    boolean isNotFiltered (Instance instance) {
        Collection<Items> itemsList = Optional.of(instance)
                                              .map(Instance::getLabelsMap)
                                              .map(GcpMemorystorePlatform::asItemCollection)
                                              .orElse(emptySet());
        Collection<Items> includeFilterItems = getIncludeFilter();
        Collection<Items> excludeFilterItems = getExcludeFilter();
        boolean hasAllMustIncludes = includeFilter.isEmpty() || itemsList.stream()
                                                                         .anyMatch(includeFilterItems::contains);
        boolean hasNoMustNotIncludes = itemsList.stream().noneMatch(excludeFilterItems::contains);
        final boolean isNotFiltered = hasAllMustIncludes && hasNoMustNotIncludes;
        if (!isNotFiltered) {
            log.info("Instance {} filtered because of {}, {}",
                    instance.getName(),
                    kv("includeFilter", hasAllMustIncludes),
                    kv("excludeFilter", hasNoMustNotIncludes));
        }
        return isNotFiltered;
    }

    CloudRedisClient getInstanceClient () {
        try {
            return CloudRedisClient.create(CloudRedisSettings.newBuilder()
                                                             .setCredentialsProvider(computeCredentialsProvider)
                                                             .build());
        } catch (IOException e) {
            throw new ChaosException(GCP_MEMORYSTORE_GENERIC_ERROR, e);
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.SAAS;
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        throw new ChaosException(GCP_MEMORYSTORE_DOES_NOT_SUPPORT_RECYCLING);
    }

    public void setExcludeFilter (Map<String, String> excludeFilter) {
        this.excludeFilter = excludeFilter;
    }

    private boolean isReady (Instance instance) {
        return instance.getState() == Instance.State.READY;
    }

    private boolean isHA (Instance instance) {
        return instance.getTier() == Instance.Tier.STANDARD_HA;
    }

    GcpMemorystoreInstanceContainer createContainerFromInstance (Instance instance) {
        return GcpMemorystoreInstanceContainer.builder()
                                              .withHost(instance.getHost())
                                              .withDisplayName(instance.getDisplayName())
                                              .withName(instance.getName())
                                              .withLocationId(instance.getLocationId())
                                              .withPort(instance.getPort())
                                              .withPlatform(this)
                                              .build();
    }

    Iterable<Instance> getInstances (LocationName parent) {
        return getInstanceClient().listInstances(parent).iterateAll();
    }

    public String failover (GcpMemorystoreInstanceContainer container,
                            FailoverInstanceRequest.DataProtectionMode mode) throws ExecutionException, InterruptedException {
        log.info("Failover triggered for instance {}", v(DATADOG_CONTAINER_KEY, container));
        FailoverInstanceRequest failoverInstanceRequest = FailoverInstanceRequest.newBuilder().
                setName(container.getName()).setDataProtectionMode(mode).build();
        return executeFailover(failoverInstanceRequest);
    }

    String executeFailover (FailoverInstanceRequest failoverInstanceRequest) throws ExecutionException, InterruptedException {
        return getInstanceClient().failoverInstanceAsync(failoverInstanceRequest).getName();
    }

    public boolean isOperationCompleted (String operationName) {
        Operation operation = getOperationsClient().getOperation(operationName);
        return operation.getDone();
    }

    OperationsClient getOperationsClient () {
        return getInstanceClient().getOperationsClient();
    }

    public ContainerHealth isContainerRunning (GcpMemorystoreInstanceContainer container) {
        Instance instance = getInstance(container);
        if (instance == null) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return isReady(instance) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
    }

    Instance getInstance (GcpMemorystoreInstanceContainer container) {
        return getInstanceClient().getInstance(container.getName());
    }
}
