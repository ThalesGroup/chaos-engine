/*
 *    Copyright (c) 2020 Thales Group
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

import com.google.cloud.compute.v1.*;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.impl.GcpComputeInstanceContainer;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thales.chaos.services.impl.GcpComputeService.COMPUTE_PROJECT;
import static net.logstash.logback.argument.StructuredArguments.kv;

@ConditionalOnProperty("gcp.compute")
@ConfigurationProperties("gcp.compute")
@Component
public class GcpComputePlatform extends Platform {
    private static final Logger log = LoggerFactory.getLogger(GcpComputePlatform.class);
    @Autowired
    private InstanceClient instanceClient;
    @Autowired
    @Qualifier(COMPUTE_PROJECT)
    private ProjectName projectName;

    @Override
    public ApiStatus getApiStatus () {
        return null;
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return null;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        return null;
    }

    @Override
    protected List<Container> generateRoster () {
        InstanceClient.AggregatedListInstancesPagedResponse instances = instanceClient.aggregatedListInstances(
                projectName);
        return StreamSupport.stream(instances.iterateAll().spliterator(), false)
                            .map(InstancesScopedList::getInstancesList)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
                            .map(this::createContainerFromInstance)
                            .collect(Collectors.toList());
    }

    private GcpComputeInstanceContainer createContainerFromInstance (Instance instance) {
        log.info("Creating container from {}", kv("instance", instance));
        String id = instance.getId();
        String name = instance.getName();
        Tags tags = instance.getTags();
        String createdBy = Optional.of(instance)
                                   .map(Instance::getMetadata)
                                   .map(Metadata::getItemsList)
                                   .stream()
                                   .flatMap(Collection::stream)
                                   .filter(items -> items.getKey().equals("created-by"))
                                   .map(Items::getValue)
                                   .findFirst()
                                   .orElse(null);
        return GcpComputeInstanceContainer.builder()
                                          .withFirewallTags(tags.getItemsList())
                                          .withInstanceName(name)
                                          .withUniqueIdentifier(id)
                                          .withPlatform(this)
                                          .withCreatedBy(createdBy)
                                          .build();
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        return false;
    }
}
