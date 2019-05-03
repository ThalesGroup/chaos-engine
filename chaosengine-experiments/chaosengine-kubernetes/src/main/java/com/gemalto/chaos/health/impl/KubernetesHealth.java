package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.KubernetesPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "kubernetes" })
public class KubernetesHealth extends AbstractPlatformHealth {
    @Autowired
    private KubernetesPlatform kubernetesPlatform;

    KubernetesHealth (KubernetesPlatform kubernetesPlatform) {
        this();
        this.kubernetesPlatform = kubernetesPlatform;
    }

    @Autowired
    KubernetesHealth () {
        log.debug("Using Kubernetes API check for health check.");
    }

    @Override
    Platform getPlatform () {
        return kubernetesPlatform;
    }
}
