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

import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ControllerKind;
import com.thales.chaos.platform.impl.KubernetesPlatform;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.Callable;

import static com.thales.chaos.exception.enums.KubernetesChaosErrorCode.POD_HAS_NO_CONTAINERS;
import static com.thales.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;

public class KubernetesPodContainer extends Container {
    @Identifier(order = 0)
    private String uuid;
    @Identifier(order = 1)
    private String podName;
    @Identifier(order = 2)
    private String namespace;
    private final Map<String, String> labels = new HashMap<>();
    private boolean isBackedByController = false;
    private KubernetesPlatform kubernetesPlatform;
    @Identifier(order = 3)
    private ControllerKind ownerKind;
    @Identifier(order = 4)
    private String ownerName;
    private Collection<String> subcontainers = new HashSet<>();
    private String targetedSubcontainer;
    private final Callable<ContainerHealth> replicaSetRecovered = () -> kubernetesPlatform.replicaSetRecovered(this);
    private String parentNode;

    private KubernetesPodContainer () {
        super();
    }

    public static KubernetesPodContainerBuilder builder () {
        return KubernetesPodContainerBuilder.aKubernetesPodContainer();
    }

    public String getUuid () {
        return uuid;
    }

    public String getPodName () {
        return podName;
    }

    public String getNamespace () {
        return namespace;
    }

    public ControllerKind getOwnerKind () {
        return ownerKind;
    }

    public String getOwnerName () {
        return ownerName;
    }

    public String getParentNode () {
        return parentNode;
    }

    @Override
    public void startExperiment (Experiment experiment) {
        this.targetedSubcontainer = null;
        super.startExperiment(experiment);
    }

    @Override
    public boolean isCattle () {
        return isBackedByController;
    }

    public String getTargetedSubcontainer () {
        return Optional.ofNullable(targetedSubcontainer)
                       .orElseGet(() -> (targetedSubcontainer = subcontainers.stream()
                                                                             .min(Comparator.comparingInt(i -> new Random()
                                                                                     .nextInt()))
                                                                             .orElseThrow(POD_HAS_NO_CONTAINERS.asChaosException())));
    }

    @Override
    public boolean canExperiment () {
        return (isBackedByController) && super.canExperiment();
    }

    @Override
    public Platform getPlatform () {
        return kubernetesPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return kubernetesPlatform.checkHealth(this);
    }

    @Override
    public String getSimpleName () {
        return String.format("%s (%s)", podName, namespace);
    }

    @Override
    public String getAggregationIdentifier () {
        return Optional.ofNullable(ownerName).orElse(podName);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withValue(podName);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(podName);
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void deletePod (Experiment experiment) {
        experiment.setCheckContainerHealth(replicaSetRecovered);
        kubernetesPlatform.deletePod(this);
    }

    public static final class KubernetesPodContainerBuilder {
        private String uuid;
        private final Map<String, String> labels = new HashMap<>();
        private final Map<String, String> dataDogTags = new HashMap<>();
        private String podName;
        private String namespace;
        private boolean isBackedByController = false;
        private KubernetesPlatform kubernetesPlatform;
        private String ownerKind;
        private String ownerName;
        private Collection<String> subcontainers;
        private String parentNode;

        private KubernetesPodContainerBuilder () {
        }

        static KubernetesPodContainerBuilder aKubernetesPodContainer () {
            return new KubernetesPodContainerBuilder();
        }

        public KubernetesPodContainerBuilder withPodName (String podName) {
            this.podName = podName;
            return this.withDataDogTag(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, podName);
        }

        public KubernetesPodContainerBuilder withDataDogTag (String key, String value) {
            dataDogTags.put(key, value);
            return this;
        }

        public KubernetesPodContainerBuilder withLabels (Map<String, String> labels) {
            this.labels.putAll(labels);
            return this;
        }

        public KubernetesPodContainerBuilder withNamespace (String namespace) {
            this.namespace = namespace;
            return this;
        }

        public KubernetesPodContainerBuilder isBackedByController (boolean isBackedByController) {
            this.isBackedByController = isBackedByController;
            return this;
        }

        public KubernetesPodContainerBuilder withKubernetesPlatform (KubernetesPlatform platform) {
            this.kubernetesPlatform = platform;
            return this;
        }

        public KubernetesPodContainerBuilder withOwnerKind (String ownerKind) {
            this.ownerKind = ownerKind;
            return this;
        }

        public KubernetesPodContainerBuilder withOwnerName (String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public KubernetesPodContainerBuilder withSubcontainers (Collection<String> subcontainers) {
            this.subcontainers = subcontainers;
            return this;
        }

        public KubernetesPodContainerBuilder withUUID (String uuid) {
            this.uuid = uuid;
            return this;
        }

        public KubernetesPodContainerBuilder withParentNode (String parentNode) {
            this.parentNode = parentNode;
            return this;
        }

        public KubernetesPodContainer build () {
            KubernetesPodContainer kubernetesPodContainer = new KubernetesPodContainer();
            kubernetesPodContainer.uuid = this.uuid;
            kubernetesPodContainer.podName = this.podName;
            kubernetesPodContainer.namespace = this.namespace;
            kubernetesPodContainer.isBackedByController = this.isBackedByController;
            kubernetesPodContainer.labels.putAll(this.labels);
            kubernetesPodContainer.dataDogTags.putAll(this.dataDogTags);
            kubernetesPodContainer.kubernetesPlatform = this.kubernetesPlatform;
            kubernetesPodContainer.ownerKind = ControllerKind.mapFromString(this.ownerKind);
            kubernetesPodContainer.ownerName = ownerName;
            kubernetesPodContainer.subcontainers = this.subcontainers;
            kubernetesPodContainer.parentNode = this.parentNode;
            try {
                kubernetesPodContainer.setMappedDiagnosticContext();
                kubernetesPodContainer.log.info("Created new Kubernetes Pod Container object");
            } finally {
                kubernetesPodContainer.clearMappedDiagnosticContext();
            }
            return kubernetesPodContainer;
        }
    }
}
