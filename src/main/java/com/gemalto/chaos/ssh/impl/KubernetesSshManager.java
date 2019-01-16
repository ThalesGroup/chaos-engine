package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.ssh.SshManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KubernetesSshManager extends SshManager {
    private static final Logger log = LoggerFactory.getLogger(KubernetesSshManager.class);
    private String podname, namespace;

    public KubernetesSshManager (String podname, String namespace) {
        super("", "");
        this.podname = podname;
        this.namespace = namespace;
    }

    public String getPodname () {
        return podname;
    }

    public String getNamespace () {
        return namespace;
    }
}
