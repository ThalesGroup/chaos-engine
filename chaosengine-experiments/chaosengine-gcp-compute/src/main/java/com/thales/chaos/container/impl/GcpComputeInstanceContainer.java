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

package com.thales.chaos.container.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentScope;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.GcpComputePlatform;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class GcpComputeInstanceContainer extends Container {
    public static final String DATADOG_IDENTIFIER_KEY = "placeholder"; // TODO Better Identifier
    @JsonProperty
    @Identifier(order = 0)
    private String uniqueIdentifier;
    @JsonProperty
    @Identifier(order = 1)
    private String instanceName;
    private GcpComputePlatform platform;
    @JsonProperty
    private String createdBy;
    @JsonProperty
    @Identifier(order = 2)
    private String zone;
    private GcpComputeInstanceContainer () {
    }

    public static GcpComputeInstanceContainerBuilder builder () {
        return new GcpComputeInstanceContainerBuilder();
    }

    public String getUniqueIdentifier () {
        return uniqueIdentifier;
    }

    void setUniqueIdentifier (String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE,
                     experimentScope = ExperimentScope.CATTLE,
                     maximumDurationInSeconds = 600)
    public void recreateInstanceInGroup (Experiment experiment) {
        AtomicReference<String> operationId = new AtomicReference<>();
        experiment.setCheckContainerHealth(() -> operationId.get() != null && platform.isOperationComplete(operationId.get()) && platform
                .isContainerGroupAtCapacity(this) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
        experiment.setSelfHealingMethod(() -> {
        });
        experiment.setFinalizeMethod(() -> {
            String newUUID = platform.getLatestInstanceId(getInstanceName(), getZone());
            setUniqueIdentifier(newUUID);
        });
        operationId.set(platform.recreateInstanceInInstanceGroup(this));
    }

    public String getInstanceName () {
        return instanceName;
    }

    public String getZone () {
        return zone;
    }

    @Override
    public Platform getPlatform () {
        return platform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return platform.isContainerRunning(this);
    }

    @Override
    public String getSimpleName () {
        return instanceName;
    }

    @Override
    public String getAggregationIdentifier () {
        return Stream.of(createdBy, instanceName)
                     .filter(Objects::nonNull)
                     .filter(not(String::isBlank))
                     .findFirst()
                     .orElse(uniqueIdentifier);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey(DATADOG_IDENTIFIER_KEY).withValue(instanceName);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier != null && uniqueIdentifier.equals(this.uniqueIdentifier);
    }

    @Override
    public boolean isCattle () {
        return createdBy != null;
    }

    @ChaosExperiment(experimentType = ExperimentType.NETWORK, experimentScope = ExperimentScope.PET)
    public void removeNetworkTags (Experiment experiment) {
        GcpComputePlatform.Fingerprint<List<String>> listFingerprint = fetchFirewallTags();
        List<String> originalTags = listFingerprint.get();
        String oldFingerprint = listFingerprint.getFingerprint();
        AtomicReference<String> operationId = new AtomicReference<>();
        experiment.setCheckContainerHealth(() -> isOperationComplete(operationId) && platform.checkTags(this,
                originalTags) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
        experiment.setSelfHealingMethod(() -> platform.setTags(this, originalTags));
        operationId.set(platform.setTagsSafe(this, List.of(), oldFingerprint));
    }

    private GcpComputePlatform.Fingerprint<List<String>> fetchFirewallTags () {
        return platform.getFirewallTags(this);
    }

    private boolean isOperationComplete (AtomicReference<String> operationId) {
        return operationId.get() != null && platform.isOperationComplete(operationId.get());
    }

    @ChaosExperiment(experimentType = ExperimentType.RESOURCE, maximumDurationInSeconds = 600)
    public void simulateMaintenance (Experiment experiment) {
        AtomicReference<String> operationId = new AtomicReference<>();
        experiment.setCheckContainerHealth(() -> isOperationComplete(operationId) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
        experiment.setSelfHealingMethod(() -> {
        });
        operationId.set(platform.simulateMaintenance(this));
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE, experimentScope = ExperimentScope.PET)
    public void stopInstance (Experiment experiment) {
        AtomicReference<String> operationId = new AtomicReference<>();
        experiment.setCheckContainerHealth(() -> isOperationComplete(operationId) ? platform.isContainerRunning(this) : ContainerHealth.RUNNING_EXPERIMENT);
        experiment.setSelfHealingMethod(() -> platform.startInstance(this));
        operationId.set(platform.stopInstance(this));
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE, experimentScope = ExperimentScope.PET)
    public void resetInstance (Experiment experiment) {
        AtomicReference<String> operationId = new AtomicReference<>();
        experiment.setCheckContainerHealth(() -> isOperationComplete(operationId) ? platform.isContainerRunning(this) : ContainerHealth.RUNNING_EXPERIMENT);
        experiment.setSelfHealingMethod(() -> platform.startInstance(this));
        operationId.set(platform.restartContainer(this));
    }

    public static class GcpComputeInstanceContainerBuilder {
        private String uniqueIdentifier;
        private String instanceName;
        private GcpComputePlatform platform;
        private String createdBy;
        private String zone;

        private GcpComputeInstanceContainerBuilder () {
        }

        public GcpComputeInstanceContainerBuilder withUniqueIdentifier (String uniqueIdentifier) {
            this.uniqueIdentifier = uniqueIdentifier;
            return this;
        }

        public GcpComputeInstanceContainerBuilder withInstanceName (String instanceName) {
            this.instanceName = instanceName;
            return this;
        }

        public GcpComputeInstanceContainerBuilder withPlatform (GcpComputePlatform platform) {
            this.platform = platform;
            return this;
        }

        public GcpComputeInstanceContainer build () {
            GcpComputeInstanceContainer container = new GcpComputeInstanceContainer();
            container.uniqueIdentifier = this.uniqueIdentifier;
            container.instanceName = this.instanceName;
            container.platform = this.platform;
            container.createdBy = this.createdBy;
            container.zone = this.zone;
            return container;
        }

        public GcpComputeInstanceContainerBuilder withCreatedBy (String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public GcpComputeInstanceContainerBuilder withZone (String zone) {
            this.zone = zone;
            return this;
        }
    }
}
