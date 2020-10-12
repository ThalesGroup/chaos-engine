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

import com.google.gson.JsonSyntaxException;
import com.thales.chaos.constants.KubernetesConstants;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.KubernetesPodContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import junit.framework.TestCase;
import org.apache.http.HttpStatus;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.joda.time.DateTime;
import org.junit.Assert;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;

import static com.thales.chaos.platform.enums.ControllerKind.REPLICA_SET;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KubernetesPlatformTest {
    private static final String POD_NAME = "mypod";
    private static final String NAMESPACE_NAME = "mynamespace";
    private static final String UUID = randomUUID().toString();
    @SpyBean
    private ContainerManager containerManager;
    @Autowired
    private KubernetesPlatform platform;
    @Mock
    private CoreApi coreApi;
    @Mock
    private CoreV1Api coreV1Api;
    @Autowired
    private ApiClient apiClient;
    @Mock
    private AppsV1Api appsV1Api;

    private static V1PodList getV1PodList (boolean isBackedByController, int numberOfPods) {
        List<V1OwnerReference> ownerReferences = null;
        if (isBackedByController) {
            ownerReferences = new ArrayList<>();
            ownerReferences.add(new V1OwnerReferenceBuilder().withNewController("mycontroller").build());
        }
        V1ObjectMeta metadata = new V1ObjectMetaBuilder().withUid(randomUUID().toString())
                                                         .withName(POD_NAME)
                                                         .withNamespace(NAMESPACE_NAME)
                                                         .withLabels(new HashMap<>())
                                                         .withOwnerReferences(ownerReferences)
                                                         .build();
        V1Pod pod = new V1Pod();
        pod.setMetadata(metadata);
        pod.setSpec(new V1PodSpec().containers(Collections.singletonList(new V1Container().name(randomUUID().toString()))));
        V1PodList list = new V1PodList();
        for (int i = 0; i < numberOfPods; i++) list.addItemsItem(pod);
        return list;
    }

    @Test
    public void testPodWithoutOwnerCannotBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(false));
        assertEquals(1, platform.getRoster().size());
        assertFalse(platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testContainerHealthWithException () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(false)
                                                     .endContainerStatus()
                                                     .build();
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenThrow(new ApiException(HttpStatus.SC_NOT_FOUND,
                "Not Found")).thenThrow(new ApiException(HttpStatus.SC_FORBIDDEN, "Forbidden"));
        assertEquals("Container no more exists",
                ContainerHealth.DOES_NOT_EXIST,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
        assertEquals("Undefined exception",
                ContainerHealth.RUNNING_EXPERIMENT,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenThrow(new ApiException());
    }

    @Before
    public void setUp () {
        doReturn(coreApi).when(platform).getCoreApi();
        doReturn(coreV1Api).when(platform).getCoreV1Api();
        doReturn(appsV1Api).when(platform).getAppsV1Api();
        platform.setNamespaces(NAMESPACE_NAME);
    }

    @Test
    public void testApiStatus () throws ApiException {
        V1APIVersions v1APIVersions = new V1APIVersionsBuilder().addToVersions("1")
                                                                .withApiVersion("apiVersion")
                                                                .withKind("kind")
                                                                .build();
        doReturn(v1APIVersions).when(coreApi).getAPIVersions();
        Assert.assertEquals(ApiStatus.OK, platform.getApiStatus());
    }

    @Test
    public void testApiStatusNotAvailable () throws ApiException {
        when(coreApi.getAPIVersions()).thenThrow(new ApiException());
        Assert.assertEquals(ApiStatus.ERROR, platform.getApiStatus());
    }

    @Test
    public void testPlatformHealth () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        assertEquals(PlatformHealth.OK, platform.getPlatformHealth());
    }

    @Test
    public void testPodWithOwnerCanBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(platform.getDestructionProbability()).thenReturn(1D);
        assertEquals(1, platform.getRoster().size());
        assertTrue(platform.getRoster().get(0).canExperiment());
    }

    @Test
    public void testContainerHealthWithOneContainerHealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(true)
                                                     .endContainerStatus()
                                                     .build();
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.NORMAL,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testDeletePod () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeletePodAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenThrow(new ApiException());
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertFalse(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeletePodJSONSyntaxException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenThrow(new JsonSyntaxException(""));
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRoasterWithAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenThrow(new ApiException());
        assertEquals(0, platform.getRoster().size());
    }

    @Test
    public void testPlatformHealthNoNamespacesToTest () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true, 0));
        assertEquals(PlatformHealth.DEGRADED, platform.getPlatformHealth());
    }

    @Test
    public void testCheckDesiredReplicasReplicationController () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        //Test ReplicationController
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                .withReplicas(1)
                .withReadyReplicas(0)
                .build()).build())
                            .thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                                    .withReplicas(1)
                                    .withReadyReplicas(1)
                                    .build()).build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testPodExists () throws ApiException {
        V1PodList v1PodList = getV1PodList(true);
        V1Pod pod = v1PodList.getItems().get(0);
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pod);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(v1PodList)
                              .thenReturn(new V1PodList())
                              .thenThrow(new ApiException(new IOException()));
        assertTrue("POD exists", platform.podExists(kubernetesPodContainer));
        assertFalse("POD does not exist", platform.podExists(kubernetesPodContainer));
        assertFalse("IO Exception", platform.podExists(kubernetesPodContainer));
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

    @Test
    public void testContainerHealthWithOneContainerUnHealthy () throws ApiException {
        V1PodStatus status = new V1PodStatusBuilder().addNewContainerStatus()
                                                     .withReady(false)
                                                     .endContainerStatus()
                                                     .build();
        V1Pod pod = getV1PodList(true).getItems().get(0);
        pod.setStatus(status);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testGetPlatformLevel () {
        assertEquals(PlatformLevel.PAAS, platform.getPlatformLevel());
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
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.NORMAL,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
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
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT,
                platform.checkHealth((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testContainerHealthDoesNotExist () throws ApiException {
        V1PodList list = new V1PodList();
        V1Pod pod = mock(V1Pod.class);
        V1ObjectMeta metadata = mock(V1ObjectMeta.class);
        V1PodSpec spec = mock(V1PodSpec.class);
        when(metadata.getUid()).thenReturn(randomUUID().toString());
        when(pod.getMetadata()).thenReturn(metadata);
        when(pod.getSpec()).thenReturn(spec);
        list.addItemsItem(pod);
        KubernetesPodContainer kubernetesPodContainer = KubernetesPodContainer.builder()
                                                                              .withNamespace(NAMESPACE_NAME)
                                                                              .withUUID(randomUUID().toString())
                                                                              .withOwnerKind("")
                                                                              .build();
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(list);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
    }

    private static V1PodList getV1PodList (boolean isBackedByController) {
        return getV1PodList(isBackedByController, 1);
    }

    @Test
    public void testCheckDesiredReplicasReplicaSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicaSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedReplicaSetStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicaSetBuilder().withStatus(new V1ReplicaSetStatusBuilder().withReplicas(
                1).withReadyReplicas(0).build()).build())
                            .thenReturn(new V1ReplicaSetBuilder().withStatus(new V1ReplicaSetStatusBuilder().withReplicas(
                                    1).withReadyReplicas(1).build()).build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasStatefulSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("StatefulSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedStatefulSetStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1StatefulSetBuilder().withStatus(new V1StatefulSetStatusBuilder().withReplicas(
                1).withReadyReplicas(0).build()).build())
                            .thenReturn(new V1StatefulSetBuilder().withStatus(new V1StatefulSetStatusBuilder().withReplicas(
                                    1).withReadyReplicas(1).build()).build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasDeployment () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("Deployment");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDeploymentStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1DeploymentBuilder().withStatus(new V1DeploymentStatusBuilder().withReplicas(
                1).withReadyReplicas(0).build()).build())
                            .thenReturn(new V1DeploymentBuilder().withStatus(new V1DeploymentStatusBuilder().withReplicas(
                                    1).withReadyReplicas(1).build()).build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasDaemonSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("DaemonSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDaemonSetStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1DaemonSetBuilder().withStatus(new V1DaemonSetStatusBuilder().withDesiredNumberScheduled(
                1).withCurrentNumberScheduled(0).build()).build())
                            .thenReturn(new V1DaemonSetBuilder().withStatus(new V1DaemonSetStatusBuilder().withDesiredNumberScheduled(
                                    1).withCurrentNumberScheduled(1).build()).build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasJob () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("Job");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasException () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("DaemonSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDaemonSetStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenThrow(new ApiException());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasCronJob () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("CronJob");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasUnsupportedKind () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("Unsupported");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void replicaSetRecovered () throws ApiException {
        V1PodList pods = getV1PodList(randomUUID().toString());
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                .withReplicas(1)
                .withReadyReplicas(1)
                .build()).build());
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pods.getItems().get(0));
        assertEquals(ContainerHealth.NORMAL, platform.replicaSetRecovered(kubernetesPodContainer));
    }

    private static V1PodList getV1PodList (String UUID) {
        List<V1OwnerReference> ownerReferences = new ArrayList<>();
        ownerReferences.add(new V1OwnerReferenceBuilder().withNewController("mycontroller").build());
        V1ObjectMeta metadata = new V1ObjectMetaBuilder().withUid(UUID)
                                                         .withName(POD_NAME)
                                                         .withNamespace(NAMESPACE_NAME)
                                                         .withLabels(new HashMap<>())
                                                         .withOwnerReferences(ownerReferences)
                                                         .build();
        V1Pod pod = new V1Pod();
        pod.setMetadata(metadata);
        pod.setSpec(new V1PodSpec().containers(Collections.singletonList(new V1Container().name(randomUUID().toString()))));
        V1PodList list = new V1PodList();
        list.addItemsItem(pod);
        return list;
    }

    @Test
    public void replicaSetRecoveredContainerExists () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                .withReplicas(1)
                .withReadyReplicas(1)
                .build()).build());
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pods.getItems().get(0));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
    }

    @Test
    public void replicaSetRecoveredNotInDesiredState () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                .withReplicas(1)
                .withReadyReplicas(0)
                .build()).build());
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pods.getItems().get(0));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"),
                eq(pods.getItems().get(0).getMetadata().getNamespace()),
                eq("true"))).thenReturn(new V1ReplicationControllerBuilder().withStatus(new V1ReplicationControllerStatusBuilder()
                .withReplicas(2)
                .withReadyReplicas(1)
                .build()).build());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
    }

    @Test
    public void checkHealthNullPointerExceptionTest () throws Exception {
        final String podName = randomUUID().toString();
        final String namespace = randomUUID().toString();
        final KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        doReturn(UUID).when(kubernetesPodContainer).getUuid();
        doReturn(podName).when(kubernetesPodContainer).getPodName();
        doReturn(namespace).when(kubernetesPodContainer).getNamespace();
        final V1Pod v1Pod = mock(V1Pod.class);
        final V1PodStatus v1PodStatus = mock(V1PodStatus.class);
        final V1ContainerStatus v1ContainerStatus = mock(V1ContainerStatus.class);
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(UUID));
        doReturn(null, v1Pod).when(coreV1Api).readNamespacedPodStatus(podName, namespace, "true");
        doReturn(null, v1PodStatus).when(v1Pod).getStatus();
        doReturn(null, Collections.singletonList(v1ContainerStatus)).when(v1PodStatus).getContainerStatuses();
        doReturn(false, true).when(v1ContainerStatus).getReady();
        assertEquals("Null pod", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
        assertEquals("Null pod status", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
        assertEquals("Null container statuses",
                ContainerHealth.DOES_NOT_EXIST,
                platform.checkHealth(kubernetesPodContainer));
        assertEquals("No ready containers",
                ContainerHealth.RUNNING_EXPERIMENT,
                platform.checkHealth(kubernetesPodContainer));
        assertEquals("Containers ready", ContainerHealth.NORMAL, platform.checkHealth(kubernetesPodContainer));
    }

    @Test
    public void testIsContainerRecycled () throws ApiException {
        String uid = randomUUID().toString();
        String containerName = randomUUID().toString();
        KubernetesPodContainer kubernetesPodContainer = spy(KubernetesPodContainer.builder()
                                                                                  .withNamespace(NAMESPACE_NAME)
                                                                                  .withUUID(uid)
                                                                                  .withSubcontainers(List.of(
                                                                                          containerName))
                                                                                  .build());
        V1ObjectMeta podMetadata = new V1ObjectMeta().uid(uid);
        V1ContainerState state = new V1ContainerState().running(new V1ContainerStateRunning().startedAt(DateTime.now()
                                                                                                                .minusMinutes(
                                                                                                                        1)));
        V1PodStatus podStatus = new V1PodStatus().containerStatuses(List.of(new V1ContainerStatus().name(containerName)
                                                                                                   .state(state)));
        V1Pod pod = new V1Pod().metadata(podMetadata).status(podStatus);
        doReturn(new V1PodList().items(List.of(pod))).when(coreV1Api)
                                                     .listNamespacedPod(NAMESPACE_NAME,
                                                             "true",
                                                             false,
                                                             "",
                                                             "",
                                                             "",
                                                             0,
                                                             "",
                                                             0,
                                                             false);
        doReturn(pod).when(coreV1Api).readNamespacedPodStatus(any(), any(), any());
        doReturn(Instant.now().minusSeconds(100)).when(kubernetesPodContainer).getExperimentStartTime();
        assertTrue(platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test
    public void testContainerIsNotRecycledYet () throws ApiException {
        String uid = randomUUID().toString();
        String containerName = randomUUID().toString();
        KubernetesPodContainer kubernetesPodContainer = spy(KubernetesPodContainer.builder()
                                                                                  .withSubcontainers(List.of(containerName))
                                                                                  .build());
        V1ObjectMeta podMetadata = new V1ObjectMeta().uid(uid);
        V1ContainerState state = new V1ContainerState().running(new V1ContainerStateRunning().startedAt(DateTime.now().minusMinutes(10)));
        V1PodStatus podStatus = new V1PodStatus().containerStatuses(List.of(new V1ContainerStatus().name(containerName).state(state)));
        V1Pod pod = new V1Pod().metadata(podMetadata).status(podStatus);
        doReturn(pod).when(coreV1Api).readNamespacedPodStatus(any(), any(), any());
        doReturn(Instant.now().minusSeconds(60)).when(kubernetesPodContainer).getExperimentStartTime();
        assertFalse("Not restarted", platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test
    public void testIsContainerRecycledNotFound () throws ApiException {
        KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        V1ReplicaSet replicaSet = new V1ReplicaSetBuilder().withStatus(new V1ReplicaSetStatusBuilder().withReplicas(1)
                                                                                                      .withReadyReplicas(
                                                                                                              1)
                                                                                                      .build()).build();
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(new V1PodList());
        when(kubernetesPodContainer.getOwnerKind()).thenReturn(REPLICA_SET);
        when(appsV1Api.readNamespacedReplicaSetStatus(any(), any(), any())).thenReturn(replicaSet);
        when(coreV1Api.readNamespacedPodStatus(any(),
                any(),
                any())).thenThrow(new ApiException(KubernetesConstants.KUBERNETES_POD_NOT_FOUND_ERROR_MESSAGE));
        assertEquals("Restarted", true, platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test(expected = ChaosException.class)
    public void testIsContainerRecycledAPIError () throws ApiException {
        String subContainer = randomUUID().toString();
        KubernetesPodContainer kubernetesPodContainer = KubernetesPodContainer.builder()
                                                                              .withOwnerKind(REPLICA_SET.toString())
                                                                              .withSubcontainers(Set.of(subContainer))
                                                                              .build();
        doReturn(true).when(platform).podExists(kubernetesPodContainer);
        doThrow(new ApiException()).when(coreV1Api).readNamespacedPodStatus(any(), any(), any());
        platform.isContainerRecycled(kubernetesPodContainer);
    }

    @Test
    public void testRecycleContainer () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyInt(),
                anyBoolean())).thenReturn(getV1PodList(true));
        KubernetesPodContainer container = mock(KubernetesPodContainer.class);
        platform.recycleContainer(container);
        verify(platform, times(1)).deletePod(container);
    }

    @Test
    public void testGetConnectedShellClient () throws IOException, ApiException {
        KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        platform.getConnectedShellClient(kubernetesPodContainer);
    }

    @Test
    public void testSetNamespaces () {
        assertThat(platform.getNamespaces(),
                IsIterableContainingInAnyOrder.containsInAnyOrder(KubernetesPlatform.DEFAULT_NAMESPACE));
        platform.setNamespaces("");
        assertThat(platform.getNamespaces(),
                IsIterableContainingInAnyOrder.containsInAnyOrder(KubernetesPlatform.DEFAULT_NAMESPACE));
        platform.setNamespaces("default,application");
        assertThat(platform.getNamespaces(),
                IsIterableContainingInAnyOrder.containsInAnyOrder("default", "application"));
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @MockBean
        private ApiClient apiClient;

        @Bean
        KubernetesPlatform kubernetesPlatform () {
            KubernetesPlatform platform = new KubernetesPlatform(apiClient);
            return Mockito.spy(platform);
        }
    }

    private class TestProcess extends Process {
        private final InputStream is;

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
        public int waitFor () {
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