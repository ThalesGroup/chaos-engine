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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.JsonSyntaxException;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.KubernetesPodContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.ShellBasedExperiment;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.shellclient.ShellClient;
import com.thales.chaos.shellclient.impl.KubernetesShellClient;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.thales.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static com.thales.chaos.exception.enums.KubernetesChaosErrorCode.K8S_API_ERROR;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty("kubernetes")
@ConfigurationProperties("kubernetes")
public class KubernetesPlatform extends Platform implements ShellBasedExperiment<KubernetesPodContainer> {
    @Autowired
    private ContainerManager containerManager;
    @Autowired
    private final ApiClient apiClient;
    private final String DEFAULT_NAMESPACE = "default";
    private Collection<String> namespaces = List.of(DEFAULT_NAMESPACE);

    public void setNamespaces (String namespaces) {
        this.namespaces = Optional.of(Arrays.stream(namespaces.split(","))
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList()))
                                  .filter(l -> !l.isEmpty())
                                  .orElse(List.of(DEFAULT_NAMESPACE));
    }

    @Autowired
    KubernetesPlatform (ApiClient apiClient) {
        this.apiClient = apiClient;
        log.info("Kubernetes Platform created");
    }

    public ContainerHealth checkHealth (KubernetesPodContainer kubernetesPodContainer) {
        try {
            boolean podExists = podExists(kubernetesPodContainer);
            if (!podExists) {
                return ContainerHealth.DOES_NOT_EXIST;
            }
            V1Pod result = getCoreV1Api().readNamespacedPodStatus(kubernetesPodContainer.getPodName(),
                    kubernetesPodContainer.getNamespace(),
                    "true");
            return Optional.ofNullable(result)
                           .map(V1Pod::getStatus)
                           .map(V1PodStatus::getContainerStatuses)
                           .map(Collection::stream)
                           .map(s -> s.allMatch(V1ContainerStatus::getReady))
                           .map(aBoolean -> aBoolean ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT)
                           .orElse(ContainerHealth.DOES_NOT_EXIST);
        } catch (ApiException e) {
            log.debug("Exception when checking container health", e);
            if (HttpStatus.SC_NOT_FOUND == e.getCode()) {
                return ContainerHealth.DOES_NOT_EXIST;
            }
        }
        return ContainerHealth.RUNNING_EXPERIMENT;
    }

    boolean podExists (KubernetesPodContainer kubernetesPodContainer) {
        String podUuid = kubernetesPodContainer.getUuid();
        if (podUuid == null) {
            return false;
        }
        boolean podExists = listAllPodsInNamespace(kubernetesPodContainer.getNamespace()).getItems()
                                                                                         .stream()
                                                                                         .map(V1Pod::getMetadata)
                                                                                         .filter(Objects::nonNull)
                                                                                         .map(V1ObjectMeta::getUid)
                                                                                         .anyMatch(podUuid::equals);
        log.debug("Kubernetes POD {} exists = {}", kubernetesPodContainer.getPodName(), podExists);
        return podExists;
    }

    @JsonIgnore
    CoreV1Api getCoreV1Api () {
        return new CoreV1Api(apiClient);
    }

    private V1PodList listAllPodsInNamespace (String namespace) {
        try {
            return getCoreV1Api().listNamespacedPod(namespace, "true", false, "", "", "", 0, "", 0, false);
        } catch (ApiException e) {
            log.error("Cannot list pods in namespace {}: {} ", namespace, e.getMessage(), e);
            return new V1PodList();
        }
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            getCoreApi().getAPIVersions().getVersions();
            return ApiStatus.OK;
        } catch (ApiException e) {
            log.error("Kubernetes API health check failed", e);
            return ApiStatus.ERROR;
        }
    }

    @JsonIgnore
    CoreApi getCoreApi () {
        return new CoreApi(apiClient);
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.PAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        if (namespaces.stream()
                      .map(this::listAllPodsInNamespace)
                      .map(V1PodList::getItems)
                      .flatMap(List::stream)
                      .collect(Collectors.toList())
                      .isEmpty()) {
            log.warn("No PODs detected in specified namespaces {}", namespaces);
            return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
    }

    @Override
    protected List<Container> generateRoster () {
        final List<Container> containerList = new ArrayList<>();
        try {
            List<V1Pod> pods = namespaces.stream()
                                         .map(this::listAllPodsInNamespace)
                                         .map(V1PodList::getItems)
                                         .flatMap(List::stream)
                                         .collect(Collectors.toList());
            containerList.addAll(pods.stream().map(this::fromKubernetesAPIPod).collect(Collectors.toSet()));
            return containerList;
        } catch (Exception e) {
            log.error("Could not generate Kubernetes roster", e);
            return containerList;
        }
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        KubernetesPodContainer kubernetesPodContainer = (KubernetesPodContainer) container;
        if (podExists(kubernetesPodContainer)) return isContainerRestarted(kubernetesPodContainer,
                ((KubernetesPodContainer) container).getTargetedSubcontainer());
        return isDesiredReplicas(kubernetesPodContainer);
    }

    KubernetesPodContainer fromKubernetesAPIPod (V1Pod pod) {
        KubernetesPodContainer container = containerManager.getMatchingContainer(KubernetesPodContainer.class,
                Optional.of(pod).map(V1Pod::getMetadata).map(V1ObjectMeta::getName).orElse(null));
        if (container == null) {
            container = KubernetesPodContainer.builder()
                                              .withUUID(pod.getMetadata().getUid())
                                              .withPodName(pod.getMetadata().getName())
                                              .withNamespace(pod.getMetadata().getNamespace())
                                              .withLabels(pod.getMetadata().getLabels())
                                              .withKubernetesPlatform(this)
                                              .isBackedByController(CollectionUtils.isNotEmpty(pod.getMetadata()
                                                                                                  .getOwnerReferences()))
                                              .withOwnerKind(Optional.ofNullable(pod.getMetadata().getOwnerReferences())
                                                                     .flatMap(list -> list.stream().findFirst())
                                                                     .map(V1OwnerReference::getKind)
                                                                     .orElse(""))
                                              .withOwnerName(Optional.ofNullable(pod.getMetadata().getOwnerReferences())
                                                                     .flatMap(list -> list.stream().findFirst())
                                                                     .map(V1OwnerReference::getName)
                                                                     .orElse(""))
                                              .withSubcontainers(Optional.of(pod)
                                                                         .map(V1Pod::getSpec)
                                                                         .map(V1PodSpec::getContainers)
                                                                         .stream()
                                                                         .flatMap(Collection::stream)
                                                                         .map(V1Container::getName)
                                                                         .collect(Collectors.toList()))
                                              .build();
            log.info("Found new Kubernetes Pod Container {}", v(DATADOG_CONTAINER_KEY, container));
            containerManager.offer(container);
        } else {
            log.debug("Found existing Kubernetes Pod Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    private boolean isContainerRestarted (KubernetesPodContainer container, String subContainerName) {
        V1Pod v1Pod;
        try {
            v1Pod = getCoreV1Api().readNamespacedPodStatus(container.getPodName(), container.getNamespace(), "true");
        } catch (ApiException e) {
            if (HttpStatus.SC_NOT_FOUND == e.getCode()) {
                return replicaSetRecovered(container) == ContainerHealth.NORMAL;
            }
            throw new ChaosException(K8S_API_ERROR, e);
        }
        return Optional.of(v1Pod)
                       .map(V1Pod::getStatus)
                       .map(V1PodStatus::getContainerStatuses)
                       .stream()
                       .flatMap(Collection::stream)
                       .filter(v1ContainerStatus -> v1ContainerStatus.getName().equals(subContainerName))
                       .map(V1ContainerStatus::getState)
                       .filter(Objects::nonNull)
                       .map(V1ContainerState::getRunning)
                       .filter(Objects::nonNull)
                       .peek(v1ContainerStateRunning -> log.debug("Evaluating last restart time from {}",
                               v("v1ContainerStateRunning", v1ContainerStateRunning)))
                       .map(V1ContainerStateRunning::getStartedAt)
                       .filter(Objects::nonNull)
                       .anyMatch(dateTime -> dateTime.isAfter(container.getExperimentStartTime().toEpochMilli()));
    }

    public ContainerHealth replicaSetRecovered (KubernetesPodContainer kubernetesPodContainer) {
        return isDesiredReplicas(kubernetesPodContainer) && !podExists(kubernetesPodContainer) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
    }

    /**
     * @param instance The Kubernetes Pod Container to retrieve Owner information from
     * @return ContainerHealth
     * <p>
     * In this function we retrieve the desired vs. the actual count of replicas during an experiment.
     * Due to the nature of Kubernetes, there can be 7 different controller types backing a pod:
     * REPLICATION_CONTROLLER, REPLICA_SET, STATEFUL_SET, DAEMON_SET, DEPLOYMENT, JOB and CRON_JOB
     * (see https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/#owners-and-dependents)
     */
    public boolean isDesiredReplicas (KubernetesPodContainer instance) {
        try {
            switch (instance.getOwnerKind()) {
                case REPLICATION_CONTROLLER:
                    V1ReplicationController rc = getCoreV1Api().readNamespacedReplicationControllerStatus(instance.getOwnerName(),
                            instance.getNamespace(),
                            "true");
                    Optional<V1ReplicationControllerStatus> controllerStatus = Optional.of(rc)
                                                                                       .map(V1ReplicationController::getStatus);
                    return controllerStatus.map(V1ReplicationControllerStatus::getReplicas)
                                           .equals(controllerStatus.map(V1ReplicationControllerStatus::getReadyReplicas));
                case REPLICA_SET:
                    V1ReplicaSet replicaSet = getAppsV1Api().readNamespacedReplicaSetStatus(instance.getOwnerName(),
                            instance.getNamespace(),
                            "true");
                    Optional<V1ReplicaSetStatus> replicaSetStatus = Optional.of(replicaSet)
                                                                            .map(V1ReplicaSet::getStatus);
                    return replicaSetStatus.map(V1ReplicaSetStatus::getReplicas)
                                           .equals(replicaSetStatus.map(V1ReplicaSetStatus::getReadyReplicas));
                case STATEFUL_SET:
                    V1StatefulSet statefulSet = getAppsV1Api().readNamespacedStatefulSetStatus(instance.getOwnerName(),
                            instance.getNamespace(),
                            "true");
                    Optional<V1StatefulSetStatus> statefulSetStatus = Optional.of(statefulSet)
                                                                              .map(V1StatefulSet::getStatus);
                    return statefulSetStatus.map(V1StatefulSetStatus::getReplicas)
                                            .equals(statefulSetStatus.map(V1StatefulSetStatus::getReadyReplicas));
                case DAEMON_SET:
                    V1DaemonSet daemonSet = getAppsV1Api().readNamespacedDaemonSetStatus(instance.getOwnerName(),
                            instance.getNamespace(),
                            "true");
                    Optional<V1DaemonSetStatus> daemonSetStatus = Optional.of(daemonSet).map(V1DaemonSet::getStatus);
                    return daemonSetStatus.map(V1DaemonSetStatus::getCurrentNumberScheduled)
                                          .equals(daemonSetStatus.map(V1DaemonSetStatus::getDesiredNumberScheduled));
                case DEPLOYMENT:
                    V1Deployment deployment = getAppsV1Api().readNamespacedDeploymentStatus(instance.getOwnerName(),
                            instance.getNamespace(),
                            "true");
                    Optional<V1DeploymentStatus> deploymentStatus = Optional.of(deployment)
                                                                            .map(V1Deployment::getStatus);
                    return deploymentStatus.map(V1DeploymentStatus::getReplicas)
                                           .equals(deploymentStatus.map(V1DeploymentStatus::getReadyReplicas));
                case JOB:
                case CRON_JOB:
                    log.warn("Job containers are not supported");
                    return false;
                default:
                    log.error("Unsupported owner type");
                    return false;
            }
        } catch (ApiException e) {
            log.error("ApiException was thrown while checking desired replica count.", e);
            return false;
        }
    }

    @JsonIgnore
    AppsV1Api getAppsV1Api () {
        return new AppsV1Api(apiClient);
    }

    @Override
    public void recycleContainer (KubernetesPodContainer container) {
        deletePod(container);
    }

    public boolean deletePod (KubernetesPodContainer instance) {
        log.debug("Deleting pod {}", v(DATADOG_CONTAINER_KEY, instance));
        try {
            V1DeleteOptions deleteOptions = new V1DeleteOptionsBuilder().withGracePeriodSeconds(0L).build();
            getCoreV1Api().deleteNamespacedPod(instance.getPodName(),
                    instance.getNamespace(),
                    "false",
                    null,
                    null,
                    null,
                    "Foreground",
                    deleteOptions);
        } catch (JsonSyntaxException e1) {
            log.debug("Normal exception, see https://github.com/kubernetes-client/java/issues/86");
        } catch (ApiException e) {
            log.error("Could not delete pod", e);
            return false;
        }
        return true;
    }

    @Override
    public ShellClient getConnectedShellClient (KubernetesPodContainer container) {
        log.debug("Creating shell client into {}", v(DATADOG_CONTAINER_KEY, container));
        return KubernetesShellClient.builder().withApiClient(apiClient)
                                    .withContainerName(container.getTargetedSubcontainer())
                                    .withPodName(container.getPodName())
                                    .withNamespace(container.getNamespace())
                                    .build();
    }
}
