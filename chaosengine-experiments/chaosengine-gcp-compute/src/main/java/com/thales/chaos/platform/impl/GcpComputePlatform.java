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

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.compute.v1.*;
import com.thales.chaos.constants.GcpConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpComputeInstanceContainer;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.selfawareness.GcpComputeSelfAwareness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thales.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static com.thales.chaos.services.impl.GcpComputeService.COMPUTE_PROJECT;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

@ConditionalOnProperty("gcp.compute")
@ConfigurationProperties("gcp.compute")
@Component
public class GcpComputePlatform extends Platform {
    private static final Logger log = LoggerFactory.getLogger(GcpComputePlatform.class);
    @Autowired
    private InstanceClient instanceClient;
    @Autowired
    private InstanceGroupClient instanceGroupClient;
    @Autowired
    private InstanceGroupManagerClient instanceGroupManagerClient;
    @Autowired
    private RegionInstanceGroupClient regionInstanceGroupClient;
    @Autowired
    private RegionInstanceGroupManagerClient regionInstanceGroupManagerClient;
    @Autowired
    private ZoneOperationClient zoneOperationClient;
    @Autowired
    @Qualifier(COMPUTE_PROJECT)
    private ProjectName projectName;
    @Autowired
    private GcpComputeSelfAwareness selfAwareness;
    private Map<String, String> includeFilter = Collections.emptyMap();
    private Map<String, String> excludeFilter = Collections.emptyMap();

    @Override
    public ApiStatus getApiStatus () {
        try {
            instanceClient.aggregatedListInstances(projectName);
        } catch (RuntimeException e) {
            log.error("Caught error when evaluating API Status of Google Cloud Platform", e);
            return ApiStatus.ERROR;
        }
        return ApiStatus.OK;
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.IAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        return null;
    }

    @Override
    protected List<Container> generateRoster () {
        log.debug("Generating roster of GCP Compute instances");
        InstanceClient.AggregatedListInstancesPagedResponse instances = instanceClient.aggregatedListInstances(
                projectName);
        return StreamSupport.stream(instances.iterateAll().spliterator(), false)
                            .map(InstancesScopedList::getInstancesList)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
                            .filter(not(this::isMe))
                            .filter(this::isNotFiltered)
                            .map(this::createContainerFromInstance)
                            .peek(container -> log.info("Created container {}", v(DATADOG_CONTAINER_KEY, container)))
                            .collect(Collectors.toList());
    }

    private boolean isMe (Instance instance) {
        long id;
        try {
            id = Long.parseLong(instance.getId());
        } catch (NumberFormatException e) {
            return false;
        }
        return selfAwareness.isMe(id);
    }

    boolean isNotFiltered (Instance instance) {
        List<Items> itemsList = Optional.of(instance)
                                        .map(Instance::getMetadata)
                                        .map(Metadata::getItemsList)
                                        .orElse(emptyList());
        Collection<Items> includeFilter = getIncludeFilter();
        Collection<Items> excludeFilter = getExcludeFilter();
        boolean hasAllMustIncludes = includeFilter.isEmpty() || itemsList.stream().anyMatch(includeFilter::contains);
        boolean hasNoMustNotIncludes = itemsList.stream().noneMatch(excludeFilter::contains);
        final boolean isNotFiltered = hasAllMustIncludes && hasNoMustNotIncludes;
        if (!isNotFiltered) {
            log.info("Instance filtered because of {}, {}",
                    kv("includeFilter", hasAllMustIncludes),
                    kv("excludeFilter", hasNoMustNotIncludes));
        }
        return isNotFiltered;
    }

    GcpComputeInstanceContainer createContainerFromInstance (Instance instance) {
        String id = instance.getId();
        String name = instance.getName();
        Tags tags = instance.getTags();
        String zone = instance.getZone();
        if (zone != null && zone.contains("/")) {
            zone = zone.substring(zone.lastIndexOf('/') + 1);
        }
        String createdBy = Optional.of(instance)
                                   .map(Instance::getMetadata)
                                   .map(Metadata::getItemsList)
                                   .stream()
                                   .flatMap(Collection::stream)
                                   .filter(GcpComputePlatform::isCreatedByItem)
                                   .map(Items::getValue)
                                   .findFirst()
                                   .orElse(null);
        return GcpComputeInstanceContainer.builder()
                                          .withFirewallTags(tags != null ? tags.getItemsList() : emptyList())
                                          .withInstanceName(name)
                                          .withUniqueIdentifier(id)
                                          .withPlatform(this)
                                          .withCreatedBy(createdBy)
                                          .withZone(zone)
                                          .build();
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

    public void setExcludeFilter (Map<String, String> excludeFilter) {
        this.excludeFilter = excludeFilter;
    }

    private static boolean isCreatedByItem (Items items) {
        return items.getKey().equals(GcpConstants.CREATED_BY_METADATA_KEY);
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

    @Override
    public boolean isContainerRecycled (Container container) {
        return false;
    }

    public void stopInstance (GcpComputeInstanceContainer container) {
        log.info("Stopping instance {}", v(DATADOG_CONTAINER_KEY, container));
        ProjectZoneInstanceName instance = getProjectZoneInstanceNameOfContainer(container, projectName);
        instanceClient.stopInstance(instance);
    }

    static ProjectZoneInstanceName getProjectZoneInstanceNameOfContainer (GcpComputeInstanceContainer container,
                                                                          ProjectName projectName) {
        return ProjectZoneInstanceName.newBuilder()
                                      .setInstance(container.getUniqueIdentifier())
                                      .setZone(container.getZone())
                                      .setProject(projectName.getProject())
                                      .build();
    }

    public ContainerHealth isContainerRunning (GcpComputeInstanceContainer gcpComputeInstanceContainer) {
        ProjectZoneInstanceName instanceName = getProjectZoneInstanceNameOfContainer(gcpComputeInstanceContainer,
                projectName);
        Instance instance;
        try {
            instance = instanceClient.getInstance(instanceName);
        } catch (ApiException e) {
            return Optional.of(e).map(ApiException::getStatusCode)
                           .map(StatusCode::getCode)
                           .filter(code -> code == StatusCode.Code.NOT_FOUND)
                           .map(code -> ContainerHealth.DOES_NOT_EXIST)
                           .orElse(ContainerHealth.RUNNING_EXPERIMENT);
        }
        String status = instance.getStatus();
        switch (status) {
            case "RUNNING":
                return ContainerHealth.NORMAL;
            case "TERMINATED":
                return ContainerHealth.DOES_NOT_EXIST;
            default:
                return ContainerHealth.RUNNING_EXPERIMENT;
        }
    }

    public String simulateMaintenance (GcpComputeInstanceContainer container) {
        log.info("Simulating Host Maintenance for Google Compute instance {}", v(DATADOG_CONTAINER_KEY, container));
        final Operation operation = instanceClient.simulateMaintenanceEventInstance(
                getProjectZoneInstanceNameOfContainer(container, projectName));
        return operation.getSelfLink();
    }

    public void setTags (GcpComputeInstanceContainer container, List<String> tags) {
        log.info("Setting tags of instance {} to {}", v(DATADOG_CONTAINER_KEY, container), tags);
        ProjectZoneInstanceName instance = getProjectZoneInstanceNameOfContainer(container, projectName);
        Tags newTags = Tags.newBuilder().addAllItems(tags).build();
        instanceClient.setTagsInstance(instance, newTags);
    }

    public boolean checkTags (GcpComputeInstanceContainer container, List<String> expectedTags) {
        log.debug("Evaluating tags of {}, expecting {}", v(DATADOG_CONTAINER_KEY, container), expectedTags);
        ProjectZoneInstanceName instance = getProjectZoneInstanceNameOfContainer(container, projectName);
        List<String> actualTags = instanceClient.getInstance(instance).getTags().getItemsList();
        log.debug("Actual tags are {}", actualTags);
        actualTags = new ArrayList<>(actualTags);
        expectedTags = new ArrayList<>(expectedTags);
        actualTags.sort(Comparator.naturalOrder());
        expectedTags.sort(Comparator.naturalOrder());
        return actualTags.equals(expectedTags);
    }

    public void startInstance (GcpComputeInstanceContainer container) {
        log.info("Starting instance {}", v(DATADOG_CONTAINER_KEY, container));
        ProjectZoneInstanceName instance = getProjectZoneInstanceNameOfContainer(container, projectName);
        instanceClient.startInstance(instance);
    }

    public boolean isContainerGroupAtCapacity (GcpComputeInstanceContainer container) {
        log.debug("Checking group actual size vs desired size for {}", v(DATADOG_CONTAINER_KEY, container));
        String group = getContainerGroup(container);
        if (ProjectZoneInstanceGroupName.isParsableFrom(group)) {
            return isContainerZoneGroupAtDesiredCapacity(container);
        } else if (ProjectRegionInstanceGroupName.isParsableFrom(group)) {
            return isContainerRegionGroupAtDesiredCapacity(container);
        }
        return false;
    }

    private String getContainerGroup (GcpComputeInstanceContainer container) {
        String group = container.getAggregationIdentifier();
        if (group.startsWith("projects/")) {
            group = group.substring("projects/".length());
        }
        return group;
    }

    private boolean isContainerZoneGroupAtDesiredCapacity (GcpComputeInstanceContainer container) {
        String group = getContainerGroup(container);
        ProjectZoneInstanceGroupName projectZoneInstanceGroupName = ProjectZoneInstanceGroupName.parse(group);
        ProjectZoneInstanceGroupManagerName projectZoneInstanceGroupManagerName = ProjectZoneInstanceGroupManagerName.of(
                projectZoneInstanceGroupName.getInstanceGroup(),
                projectZoneInstanceGroupName.getProject(),
                projectZoneInstanceGroupName.getZone());
        Integer actualSize = instanceGroupClient.getInstanceGroup(projectZoneInstanceGroupName).getSize();
        Integer targetSize = instanceGroupManagerClient.getInstanceGroupManager(projectZoneInstanceGroupManagerName)
                                                       .getTargetSize();
        log.debug("For group {}, {}, {}", group, kv("actualSize", actualSize), kv("targetSize", targetSize));
        return targetSize.compareTo(actualSize) <= 0;
    }

    private boolean isContainerRegionGroupAtDesiredCapacity (GcpComputeInstanceContainer container) {
        String group = getContainerGroup(container);
        ProjectRegionInstanceGroupName projectRegionInstanceGroupName = ProjectRegionInstanceGroupName.parse(group);
        ProjectRegionInstanceGroupManagerName projectRegionInstanceGroupManagerName = ProjectRegionInstanceGroupManagerName
                .of(projectRegionInstanceGroupName.getInstanceGroup(),
                        projectRegionInstanceGroupName.getProject(),
                        projectRegionInstanceGroupName.getRegion());
        Integer actualSize = regionInstanceGroupClient.getRegionInstanceGroup(projectRegionInstanceGroupName).getSize();
        Integer targetSize = regionInstanceGroupManagerClient.getRegionInstanceGroupManager(
                projectRegionInstanceGroupManagerName).getTargetSize();
        log.debug("For group {}, {}, {}", group, kv("actualSize", actualSize), kv("targetSize", targetSize));
        return targetSize.compareTo(actualSize) <= 0;
    }

    public void restartContainer (GcpComputeInstanceContainer container) {
        log.info("Restarting instance {}", v(DATADOG_CONTAINER_KEY, container));
        instanceClient.resetInstance(getProjectZoneInstanceNameOfContainer(container, projectName));
    }

    public boolean isOperationComplete (String operationId) {
        if (operationId.startsWith("https://www.googleapis.com/compute/v1/projects/")) {
            operationId = operationId.substring("https://www.googleapis.com/compute/v1/projects/".length());
        }
        log.info("Checking status of Google Compute Operation {}", operationId);
        Operation zoneOperation = zoneOperationClient.getZoneOperation(ProjectZoneOperationName.parse(operationId));
        return zoneOperation.getProgress() >= 100;
    }
}
