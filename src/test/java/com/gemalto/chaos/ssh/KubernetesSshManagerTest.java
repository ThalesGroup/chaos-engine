package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ssh.impl.KubernetesSshManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesSshManagerTest {
    private final String podName = UUID.randomUUID().toString();
    private final String namespace = UUID.randomUUID().toString();

    @Test
    public void connect () {
        KubernetesSshManager sshManager = new KubernetesSshManager(podName, namespace);
        assertEquals(podName, sshManager.getPodname());
        assertEquals(namespace, sshManager.getNamespace());
    }
}