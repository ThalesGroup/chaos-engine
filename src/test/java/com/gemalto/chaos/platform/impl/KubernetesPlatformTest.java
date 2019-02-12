package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.KubernetesPodContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.ssh.impl.experiments.ForkBomb;
import com.gemalto.chaos.ssh.services.ShResourceService;
import com.google.gson.JsonSyntaxException;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KubernetesPlatformTest {
    private static final String POD_NAME = "mypod";
    private static final String NAMESPACE_NAME = "mynamespace";

    @SpyBean
    private ContainerManager containerManager;
    @SpyBean
    private ShResourceService shResourceService;

    @Autowired
    private KubernetesPlatform platform;
    @Autowired
    private CoreApi coreApi;
    @Autowired
    private CoreV1Api coreV1Api;
    @Autowired
    private Exec exec;

    @Before
    public void setUp () {
        platform.setNamespace(NAMESPACE_NAME);
    }

    @Test
    public void testPodWithoutOwnerCannotBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(false));
        assertEquals(1, platform.getRoster().size());
        assertFalse(platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testPodWithOwnerCanBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(platform.getDestructionProbability()).thenReturn(1D);
        assertEquals(1, platform.getRoster().size());
        assertTrue(platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testApiStatus () throws ApiException {
        V1APIVersions v1APIVersions = new V1APIVersionsBuilder().addToVersions("1")
                                                                .withApiVersion("apiVersion")
                                                                .withKind("kind")
                                                                .build();
        doReturn(v1APIVersions).when(coreApi).getAPIVersions();
            assertEquals(ApiStatus.OK, platform.getApiStatus());
    }

    @Test
    public void testApiStatusNotAvailable () throws ApiException {
            when(coreApi.getAPIVersions()).thenThrow(new ApiException());
            assertEquals(ApiStatus.ERROR, platform.getApiStatus());

    }

    @Test
    public void testPlatformHealth () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
            assertEquals(PlatformHealth.OK, platform.getPlatformHealth());

    }

    @Test
    public void testContainerHealthWithOneContainerHealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .build();
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.NORMAL, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                   .get(0)));
    }

    private static V1PodList getV1PodList (boolean isBackedByController) {
        return getV1PodList(isBackedByController, 1);
    }

    @Test
    public void testDeleteContainer () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        boolean result = platform.deleteContainer((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    public void testDeleteContainerAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApiException());
        boolean result = platform.deleteContainer((KubernetesPodContainer) platform.getRoster().get(0));
        assertFalse(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeleteContainerJSONSyntaxException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any())).thenThrow(new JsonSyntaxException(""));
        boolean result = platform.deleteContainer((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRoasterWithAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException());
        assertEquals(0, platform.getRoster().size());
    }

    @Test
    public void testSshExperiment () throws ApiException, IOException {
        when(exec.exec(anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenReturn(new TestProcess(new ByteArrayInputStream("test data"
                .getBytes())));
        KubernetesPodContainer container = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        platform.sshExperiment(new ForkBomb(), container);
    }

    @Test(expected = ChaosException.class)
    public void testSshExperimentWithException () throws ApiException, IOException {
        when(exec.exec(anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenThrow(new IOException());
        KubernetesPodContainer container = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        platform.sshExperiment(new ForkBomb(), container);
    }

    @Test
    public void getGetPlatformLevel () {
        assertEquals(PlatformLevel.PAAS, platform.getPlatformLevel());
    }

    @Test
    public void testPlatformHealthNoNamespacesToTest () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true, 0));
            assertEquals(PlatformHealth.DEGRADED, platform.getPlatformHealth());
    }

    @Test
    public void testPlatformHealthNotAvailable () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException());
            assertEquals(PlatformHealth.FAILED, platform.getPlatformHealth());
    }

    @Test
    public void testCreationFromAPI () {
        KubernetesPodContainer container = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        TestCase.assertEquals(POD_NAME, container.getPodName());
        TestCase.assertEquals(NAMESPACE_NAME, container.getNamespace());
        verify(containerManager, times(1)).offer(container);
        KubernetesPodContainer container2 = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        verify(containerManager, times(1)).offer(container);
        assertSame(container, container2);
    }

    private static V1PodList getV1PodList (boolean isBackedByController, int numberOfPods) {
        List ownerReferences = new ArrayList<>();
        if (isBackedByController) {
            ownerReferences.add(new V1OwnerReferenceBuilder().withNewController("mycontroller").build());
        }
        V1ObjectMeta metadata = new V1ObjectMetaBuilder().withName(POD_NAME)
                                                         .withNamespace(NAMESPACE_NAME)
                                                         .withLabels(new HashMap<>())
                                                         .withOwnerReferences(ownerReferences)
                                                         .build();
        V1Pod pod = new V1Pod();
        pod.setMetadata(metadata);
        V1PodList list = new V1PodList();
        for (int i = 0; i < numberOfPods; i++) list.addItemsItem(pod);
        return list;
    }

    @Test
    public void testContainerHealthWithOneContainerUnHealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(false)
                                                     .endContainerStatus()
                                                     .build();
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                           .get(0)));
    }

    @Test
    public void testContainerHealthWithSeveralContainerAllHealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .build();
        assertEquals(3, status.getContainerStatuses().size());
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.NORMAL, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                   .get(0)));
    }

    @Test
    public void testContainerHealthWithSeveralContainerOneUnhealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .addNewContainerStatus()
                                                     .withReady(false)
                                                     .endContainerStatus()
                                                     .addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .build();
        assertEquals(3, status.getContainerStatuses().size());
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                           .get(0)));
    }


    @Configuration
    static class ContextConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private ShResourceService shResourceService;
        @MockBean
        private CoreApi coreApi;
        @MockBean
        private CoreV1Api coreV1Api;
        @MockBean
        private Exec exec;

        @Bean
        KubernetesPlatform kubernetesPlatform () {
            KubernetesPlatform platform = new KubernetesPlatform(coreApi, coreV1Api, exec);
            return Mockito.spy(platform);
        }
    }

    private class TestProcess extends Process {
        private InputStream is;

        public TestProcess (InputStream is) {
            this.is = is;
        }

        @Override
        public OutputStream getOutputStream () {
            return null;
        }

        @Override
        public InputStream getInputStream () {
            return is;
        }

        @Override
        public InputStream getErrorStream () {
            return null;
        }

        @Override
        public int waitFor () throws InterruptedException {
            return 0;
        }

        @Override
        public int exitValue () {
            return 0;
        }

        @Override
        public void destroy () {
        }
    }

}