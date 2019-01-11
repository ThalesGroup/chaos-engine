package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.KubernetesPodContainer;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesPlatformTest {
    @Mock
    private CoreV1Api coreV1Api;
    @Mock
    private CoreApi coreApi;
    @Mock
    private ApiClient client;
    @InjectMocks
    private KubernetesPlatform platform = new KubernetesPlatform(client);

    @Before
    public void setup () {
        platform = spy(platform);
    }

    @Test
    public void testPodWithoutOwnerCannotBeTested () throws Exception {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(getV1PodList(false));
        assertEquals(1, platform.getRoster().size());
        assertEquals(false, platform.getRoster().get(0).canExperiment());
    }

    private static final V1PodList getV1PodList (boolean isBackedByController) {
        List ownerReferences = new ArrayList<>();
        if (isBackedByController) {
            ownerReferences.add(new V1OwnerReferenceBuilder().withNewController("mycontroller").build());
        }
        V1ObjectMeta metadata = new V1ObjectMetaBuilder().withName("mypod")
                                                         .withNamespace("mynamespace")
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
    public void testRoasterWithAPIException () throws ApiException {
        when(coreV1Api.listPodForAllNamespaces(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApiException());
        assertEquals(0, platform.getRoster().size());
    }

    @Test
    public void getGetPlatformLevel () {
        assertEquals(PlatformLevel.PAAS, platform.getPlatformLevel());
    }
}