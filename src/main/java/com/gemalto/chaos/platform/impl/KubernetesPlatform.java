package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.KubernetesPodContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.ssh.KubernetesSshExperiment;
import com.gemalto.chaos.ssh.SshExperiment;
import com.gemalto.chaos.ssh.impl.KubernetesSshManager;
import com.gemalto.chaos.ssh.services.ShResourceService;
import com.google.gson.JsonSyntaxException;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1DeleteOptionsBuilder;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty("kubernetes")
@ConfigurationProperties("kubernetes")
public class KubernetesPlatform extends Platform {
    private static final Set<String> PROTECTED_NAMESPACES = new HashSet<>(Arrays.asList("kube-system"));
    @Autowired
    private ContainerManager containerManager;
    @Autowired
    private ShResourceService shResourceService;
    @Autowired
    private CoreApi coreApi;
    @Autowired
    private CoreV1Api coreV1Api;
    @Autowired
    private Exec exec;


    @Autowired
    KubernetesPlatform (CoreApi coreApi, CoreV1Api coreV1Api, Exec exec) {
        this.coreApi = coreApi;
        this.coreV1Api = coreV1Api;
        this.exec = exec;
        log.info("Kubernetes Platform created");
    }

    public boolean stopInstance (KubernetesPodContainer instance) {
        try {
            V1DeleteOptions deleteOptions = new V1DeleteOptionsBuilder().build();
            coreV1Api.deleteNamespacedPod(instance.getPodName(), instance.getNamespace(), deleteOptions, "true", null, null, null);
        } catch (JsonSyntaxException e1) {
            log.info("Normal exception, see https://github.com/kubernetes-client/java/issues/86");
        } catch (ApiException e) {
            log.error("Could not delete pod", e);
            return false;
        }
        return true;
    }

    public void sshExperiment (SshExperiment experiment, KubernetesPodContainer container) {
        try {
            KubernetesSshExperiment.fromExperiment(experiment)
                                   .setExec(exec)
                                   .setSshManager(new KubernetesSshManager(container.getPodName(), container.getNamespace()))
                                   .setShResourceService(shResourceService)
                                   .runExperiment();
        } catch (IOException e) {
            throw new ChaosException(e);
        }
    }

    public ContainerHealth checkHealth (KubernetesPodContainer instance) {
        return ContainerHealth.NORMAL;
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            coreApi.getAPIVersions().getVersions();
            return ApiStatus.OK;
        } catch (ApiException e) {
            log.error("Kubernetes API health check failed", e);
            return ApiStatus.ERROR;
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.PAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        try {
            V1PodList pods = coreV1Api.listPodForAllNamespaces("", "", true, "", 0, "", "", 0, false);
            Set<String> namespaces = pods.getItems()
                                         .stream()
                                         .filter(p -> !PROTECTED_NAMESPACES.contains(p.getMetadata().getNamespace()))
                                         .map(this::getNamespace)
                                         .collect(Collectors.toSet());
            return (namespaces.size() > 0) ? PlatformHealth.OK : PlatformHealth.DEGRADED;
        } catch (ApiException e) {
            log.error("Kubernetes Platform health check failed", e);
            return PlatformHealth.FAILED;
        }
    }

    @Override
    protected List<Container> generateRoster () {
        final List<Container> containerList = new ArrayList<>();
        try {
            V1PodList pods = coreV1Api.listPodForAllNamespaces("", "", true, "", 0, "", "", 0, false);
            containerList.addAll(pods.getItems()
                                     .stream()
                                     .filter(p -> !PROTECTED_NAMESPACES.contains(p.getMetadata().getNamespace()))
                                     .map(this::fromKubernetesAPIPod)
                                     .collect(Collectors.toSet()));
            return containerList;
        } catch (ApiException e) {
            log.error("Could not generate Kubernetes roster", e);
            return containerList;
        }
    }

    public KubernetesPodContainer fromKubernetesAPIPod (V1Pod pod) {
        KubernetesPodContainer container = containerManager.getMatchingContainer(KubernetesPodContainer.class, pod.getMetadata()
                                                                                                                  .getName());
        if (container == null) {
            container = new KubernetesPodContainer.KubernetesPodContainerBuilder().withPodName(pod.getMetadata()
                                                                                                  .getName())
                                                                                  .withNamespace(pod.getMetadata()
                                                                                                    .getNamespace())
                                                                                  .withLabels(pod.getMetadata()
                                                                                                 .getLabels())
                                                                                  .withKubernetesPlatform(this)
                                                                                  .isBackedByController(CollectionUtils.isNotEmpty(pod
                                                                                          .getMetadata()
                                                                                          .getOwnerReferences()))
                                                                                  .build();
            log.info("Found new Kubernetes Pod Container {}", v(DATADOG_CONTAINER_KEY, container));
            containerManager.offer(container);
        } else {
            log.debug("Found existing Kubernetes Pod Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    private String getNamespace (V1Pod pod) {
        return pod.getMetadata().getNamespace();
    }
}
