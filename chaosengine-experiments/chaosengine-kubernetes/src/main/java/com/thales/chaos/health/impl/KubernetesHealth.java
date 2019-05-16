package com.thales.chaos.health.impl;

import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.KubernetesPlatform;
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
