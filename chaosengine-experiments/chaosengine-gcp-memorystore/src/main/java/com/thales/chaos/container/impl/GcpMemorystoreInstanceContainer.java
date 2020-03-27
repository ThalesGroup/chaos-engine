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
import com.google.cloud.redis.v1.FailoverInstanceRequest;
import com.google.cloud.redis.v1.Instance;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentScope;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.GcpMemorystorePlatform;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ExecutionException;

import static com.thales.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;

public class GcpMemorystoreInstanceContainer extends Container {
    @JsonProperty
    @Identifier(order = 0)
    private String name;
    @JsonProperty
    @Identifier(order = 1)
    private String displayName;
    @JsonProperty
    @Identifier(order = 2)
    private String host;
    @JsonProperty
    @Identifier(order = 3)
    private int port;
    @JsonProperty
    @Identifier(order = 4)
    private String locationId;
    @JsonProperty
    @Identifier(order = 5)
    private Instance.State state;
    private GcpMemorystorePlatform platform;

    public static GcpMemorystoreInstanceContainerBuilder builder () {
        return new GcpMemorystoreInstanceContainerBuilder();
    }

    public String getName () {
        return name;
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
        String[] path = name.split("/");
        return path[path.length - 1];
    }

    @Override
    public String getAggregationIdentifier () {
        return getSimpleName() + "-" + locationId;
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withValue(getSimpleName());
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return false;
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE, experimentScope = ExperimentScope.PET)
    public void forceFailover (Experiment experiment) throws ExecutionException, InterruptedException {
        String operationName = platform.failover(this, FailoverInstanceRequest.DataProtectionMode.FORCE_DATA_LOSS);
        experiment.setCheckContainerHealth(() -> platform.isOperationCompleted(operationName) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE, experimentScope = ExperimentScope.PET)
    public void failover (Experiment experiment) throws ExecutionException, InterruptedException {
        String operationName = platform.failover(this, FailoverInstanceRequest.DataProtectionMode.LIMITED_DATA_LOSS);
        experiment.setCheckContainerHealth(() -> platform.isOperationCompleted(operationName) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
    }

    public static class GcpMemorystoreInstanceContainerBuilder {
        private String host;
        private String name;
        private String displayName;
        private int port;
        private String locationId;
        private Instance.State state;
        private GcpMemorystorePlatform platform;

        private GcpMemorystoreInstanceContainerBuilder () {
        }

        public GcpMemorystoreInstanceContainerBuilder fromInstance (Instance instance) {
            return this.withHost(instance.getHost())
                       .withDisplayName(instance.getDisplayName())
                       .withName(instance.getName())
                       .withLocationId(instance.getLocationId())
                       .withState(instance.getState())
                       .withPort(instance.getPort());
        }

        public GcpMemorystoreInstanceContainerBuilder withPort (int port) {
            this.port = port;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withState (Instance.State state) {
            this.state = state;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withLocationId (String locationId) {
            this.locationId = locationId;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withName (String name) {
            this.name = name;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withDisplayName (String displayName) {
            this.displayName = displayName;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withHost (String host) {
            this.host = host;
            return this;
        }

        public GcpMemorystoreInstanceContainerBuilder withPlatform (GcpMemorystorePlatform platform) {
            this.platform = platform;
            return this;
        }

        public GcpMemorystoreInstanceContainer build () {
            GcpMemorystoreInstanceContainer gcpMemorystoreInstanceContainer = new GcpMemorystoreInstanceContainer();
            gcpMemorystoreInstanceContainer.host = this.host;
            gcpMemorystoreInstanceContainer.locationId = this.locationId;
            gcpMemorystoreInstanceContainer.name = this.name;
            gcpMemorystoreInstanceContainer.displayName = this.displayName;
            gcpMemorystoreInstanceContainer.port = this.port;
            gcpMemorystoreInstanceContainer.state = this.state;
            gcpMemorystoreInstanceContainer.platform = this.platform;
            return gcpMemorystoreInstanceContainer;
        }
    }
}
