package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.KubernetesPlatform;
import io.kubernetes.client.models.V1Pod;
import org.apache.commons.collections.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;

public class KubernetesPodContainer extends Container {
    private String podName;
    private String namespace;
    private Map<String, String> labels = new HashMap<>();
    private boolean isBackedByController = false;
    private transient KubernetesPlatform kubernetesPlatform;

    private KubernetesPodContainer () {
        super();
    }

    public static KubernetesPodContainerBuilder builder () {
        return KubernetesPodContainerBuilder.aKubernetesPodContainer();
    }

    public static KubernetesPodContainer fromKubernetesAPIPod (V1Pod pod, KubernetesPlatform platform) {
        return new KubernetesPodContainerBuilder().withPodName(pod.getMetadata().getName())
                                                  .withNamespace(pod.getMetadata().getNamespace())
                                                  .withLabels(pod.getMetadata().getLabels())
                                                  .withKubernetesPlatform(platform)
                                                  .isBackedByController(CollectionUtils.isNotEmpty(pod.getMetadata()
                                                                                                      .getOwnerReferences()))
                                                  .build();
    }

    public String getPodName () {
        return podName;
    }

    public String getNamespace () {
        return namespace;
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
    public void stopContainer (Experiment experiment) {
        kubernetesPlatform.stopInstance(this);
        experiment.setSelfHealingMethod(() -> {
            return null;
        });
        experiment.setCheckContainerHealth(() -> {
            return ContainerHealth.NORMAL;
        });
    }

    public static final class KubernetesPodContainerBuilder {
        private final Map<String, String> labels = new HashMap<>();
        private final Map<String, String> dataDogTags = new HashMap<>();
        private String podName;
        private String namespace;
        private boolean isBackedByController = false;
        private KubernetesPlatform kubernetesPlatform;

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

        public KubernetesPodContainer build () {
            KubernetesPodContainer kubernetesPodContainer = new KubernetesPodContainer();
            kubernetesPodContainer.podName = this.podName;
            kubernetesPodContainer.namespace = this.namespace;
            kubernetesPodContainer.isBackedByController = this.isBackedByController;
            kubernetesPodContainer.labels.putAll(this.labels);
            kubernetesPodContainer.dataDogTags.putAll(this.dataDogTags);
            kubernetesPodContainer.kubernetesPlatform = this.kubernetesPlatform;
            try {
                kubernetesPodContainer.setMappedDiagnosticContext();
                kubernetesPodContainer.log.info("Created new AWS EC2 Container object");
            } finally {
                kubernetesPodContainer.clearMappedDiagnosticContext();
            }
            return kubernetesPodContainer;
        }
    }
}
