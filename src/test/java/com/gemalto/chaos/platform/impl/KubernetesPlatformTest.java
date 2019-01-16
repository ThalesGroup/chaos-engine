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
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    @Mock
    private CoreV1Api coreV1Api;
    @Mock
    private CoreApi coreApi;
    @Mock
    private Exec exec;
    @MockBean
    private ApiClient client;
    @SpyBean
    private ContainerManager containerManager;
    @SpyBean
    private ShResourceService shResourceService;
    @Autowired
    private KubernetesPlatform platform;

    @Before
    public void setup () {
        platform.setCoreV1Api(coreV1Api);
        platform.setCoreApi(coreApi);
        platform.setExec(exec);
    }

    @Test
    public void testPodWithoutOwnerCannotBeTested () throws Exception {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(false));
        assertEquals(1, platform.getRoster().size());
        assertEquals(false, platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testPodWithOwnerCanBeTested () throws Exception {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(true));
        when(platform.getDestructionProbability()).thenReturn(1D);
        assertEquals(1, platform.getRoster().size());
        assertEquals(true, platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testApiStatus () {
        try {
            when(coreApi.getAPIVersions()).thenReturn(new V1APIVersionsBuilder().addToVersions("1").build());
            assertEquals(ApiStatus.OK, platform.getApiStatus());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testApiStatusNotAvailable () {
        try {
            when(coreApi.getAPIVersions()).thenThrow(new ApiException());
            assertEquals(ApiStatus.ERROR, platform.getApiStatus());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testPlatformHealth () {
        try {
            when(coreApi.getAPIVersions()).thenReturn(new V1APIVersionsBuilder().addToVersions("1").build());
            assertEquals(PlatformHealth.OK, platform.getPlatformHealth());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testContainerHealth () {
        try {
            when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(true));
            assertEquals(ContainerHealth.NORMAL, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                       .get(0)));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testPlatformHealthNotAvailable () {
        try {
            when(coreApi.getAPIVersions()).thenThrow(new ApiException());
            assertEquals(PlatformHealth.FAILED, platform.getPlatformHealth());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testStopContainer () throws ApiException {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(true));
        boolean result = platform.stopInstance((KubernetesPodContainer) platform.getRoster().get(0));
        assertEquals(true, result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testStopContainerAPIException () throws ApiException {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApiException());
        boolean result = platform.stopInstance((KubernetesPodContainer) platform.getRoster().get(0));
        assertEquals(false, result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testStopContainerJSONSyntaxException () throws ApiException {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any())).thenThrow(new JsonSyntaxException(""));
        boolean result = platform.stopInstance((KubernetesPodContainer) platform.getRoster().get(0));
        assertEquals(true, result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRoasterWithAPIException () throws ApiException {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApiException());
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

    private static final V1PodList getV1PodList (boolean isBackedByController) {
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
        list.addItemsItem(pod);
        return list;
    }

    @Test
    public void testCreationFromAPI () throws Exception {
        KubernetesPodContainer container = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        TestCase.assertEquals(POD_NAME, container.getPodName());
        TestCase.assertEquals(NAMESPACE_NAME, container.getNamespace());
        verify(containerManager, times(1)).offer(container);
        KubernetesPodContainer container2 = platform.fromKubernetesAPIPod(getV1PodList(true).getItems().get(0));
        verify(containerManager, times(1)).offer(container);
        assertSame(container, container2);
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private ApiClient apiClient;
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private ShResourceService shResourceService;

        @Bean
        KubernetesPlatform kubernetesPlatform () {
            return Mockito.spy(new KubernetesPlatform(apiClient));
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