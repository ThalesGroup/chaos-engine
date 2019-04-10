package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ControllerKind;
import com.gemalto.chaos.platform.impl.KubernetesPlatform;
import com.google.common.base.Enums;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.Callable;

import static com.gemalto.chaos.exception.enums.KubernetesChaosErrorCode.POD_HAS_NO_CONTAINERS;
import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;

public class KubernetesPodContainer extends Container {
    private String UUID;
    private String podName;
    private String namespace;
    private Map<String, String> labels = new HashMap<>();
    private boolean isBackedByController = false;
    private transient KubernetesPlatform kubernetesPlatform;
    private ControllerKind ownerKind;
    private String ownerName;
    private Collection<String> subcontainers = new HashSet<>();
    private transient String targetedSubcontainer;
    private transient Callable<ContainerHealth> replicaSetRecovered = () -> kubernetesPlatform.replicaSetRecovered(this);

    private KubernetesPodContainer () {
        super();
    }

    public static KubernetesPodContainerBuilder builder () {
        return KubernetesPodContainerBuilder.aKubernetesPodContainer();
    }

    public String getUUID () {
        return UUID;
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
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withValue(podName);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(podName);
    }

    @StateExperiment
    public void deletePod (Experiment experiment) {
        experiment.setCheckContainerHealth(replicaSetRecovered);
        kubernetesPlatform.deletePod(this);
    }

    public static final class KubernetesPodContainerBuilder {
        private String UUID;
        private final Map<String, String> labels = new HashMap<>();
        private final Map<String, String> dataDogTags = new HashMap<>();
        private String podName;
        private String namespace;
        private boolean isBackedByController = false;
        private KubernetesPlatform kubernetesPlatform;
        private String ownerKind;
        private String ownerName;
        private Collection<String> subcontainers;

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

        public KubernetesPodContainerBuilder withUUID (String UUID) {
            this.UUID = UUID;
            return this;
        }

        public KubernetesPodContainer build () {
            KubernetesPodContainer kubernetesPodContainer = new KubernetesPodContainer();
            kubernetesPodContainer.UUID = this.UUID;
            kubernetesPodContainer.podName = this.podName;
            kubernetesPodContainer.namespace = this.namespace;
            kubernetesPodContainer.isBackedByController = this.isBackedByController;
            kubernetesPodContainer.labels.putAll(this.labels);
            kubernetesPodContainer.dataDogTags.putAll(this.dataDogTags);
            kubernetesPodContainer.kubernetesPlatform = this.kubernetesPlatform;
            kubernetesPodContainer.ownerKind = Enums.getIfPresent(ControllerKind.class, ownerKind).orNull();
            kubernetesPodContainer.ownerName = ownerName;
            kubernetesPodContainer.subcontainers = this.subcontainers;
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
