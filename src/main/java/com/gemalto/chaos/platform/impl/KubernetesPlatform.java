package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.KubernetesPodContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.google.gson.JsonSyntaxException;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1DeleteOptionsBuilder;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty("kubernetes")
@ConfigurationProperties("kubernetes")
public class KubernetesPlatform extends Platform {
    private static final List<String> PROTECTED_NAMESPACES = Arrays.asList("kube-system");
    private CoreApi coreApi;
    private CoreV1Api coreV1Api;

    @Autowired
    public KubernetesPlatform () throws IOException {
        this(Config.defaultClient());
    }

    public KubernetesPlatform (ApiClient client) {
        Configuration.setDefaultApiClient(client);
        coreApi = new CoreApi();
        coreV1Api = new CoreV1Api();
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

    public ContainerHealth checkHealth (KubernetesPodContainer instance) {
        return ContainerHealth.NORMAL;
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            coreApi.getAPIVersions().getVersions();
            return ApiStatus.OK;
        } catch (ApiException e) {
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
            coreApi.getAPIVersions().getVersions();
            return PlatformHealth.OK;
        } catch (ApiException e) {
            return PlatformHealth.FAILED;
        }
    }

    @Override
    protected List<Container> generateRoster () {
        final List<Container> containerList = new ArrayList<>();
        try {
            V1PodList pods = coreV1Api.listPodForAllNamespaces("", "", true, "", 0, "", "", 0, false);
            containerList.addAll(pods.getItems()
                                     .parallelStream()
                                     .filter(p -> !PROTECTED_NAMESPACES.contains(p.getMetadata().getNamespace()))
                                     .map(p -> KubernetesPodContainer.fromKubernetesAPIPod(p, this))
                                     .collect(Collectors.toSet()));
            return containerList;
        } catch (ApiException e) {
            log.error("Could not generate Kubernetes roster", e);
            return containerList;
        }
    }
}
