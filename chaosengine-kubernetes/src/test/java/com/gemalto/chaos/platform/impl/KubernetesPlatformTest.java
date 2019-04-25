package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.constants.KubernetesConstants;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.KubernetesPodContainer;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.ControllerKind;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.google.gson.JsonSyntaxException;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;
import junit.framework.TestCase;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    @Autowired
    private CoreApi coreApi;
    @Autowired
    private CoreV1Api coreV1Api;
    @Autowired
    private Exec exec;
    @Autowired
    private AppsV1Api appsV1Api;

    @Before
    public void setUp () {
        platform.setNamespace(NAMESPACE_NAME);
    }

    @Test
    public void testPodWithoutOwnerCannotBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(false));
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenThrow(new ApiException(HttpStatus.SC_NOT_FOUND, "Not Found"))
                                                                    .thenThrow(new ApiException(HttpStatus.SC_FORBIDDEN, "Forbidden"));
        assertEquals("Container no more exists", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth((KubernetesPodContainer) platform
                .getRoster()
                .get(0)));
        assertEquals("Undefined exception", ContainerHealth.RUNNING_EXPERIMENT, platform.checkHealth((KubernetesPodContainer) platform
                .getRoster()
                .get(0)));
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException());
        assertEquals("Error while checking container presence", ContainerHealth.RUNNING_EXPERIMENT, platform.checkHealth((KubernetesPodContainer) platform
                .getRoster()
                .get(0)));
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        assertEquals(PlatformHealth.OK, platform.getPlatformHealth());
    }

    @Test
    public void testPodWithOwnerCanBeTested () throws Exception {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.NORMAL, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                   .get(0)));
    }

    @Test
    public void testDeletePod () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeletePodAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new ApiException());
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertFalse(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testDeletePodJSONSyntaxException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new JsonSyntaxException(""));
        boolean result = platform.deletePod((KubernetesPodContainer) platform.getRoster().get(0));
        assertTrue(result);
        verify(coreV1Api, times(1)).deleteNamespacedPod(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRoasterWithAPIException () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException());
        assertEquals(0, platform.getRoster().size());
    }

    @Test
    public void testPlatformHealthNoNamespacesToTest () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true, 0));
        assertEquals(PlatformHealth.DEGRADED, platform.getPlatformHealth());
    }

    @Test
    public void testCheckDesiredReplicasReplicationController () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        //Test ReplicationController
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"), eq(pods.getItems()
                                                                                     .get(0)
                                                                                     .getMetadata()
                                                                                     .getNamespace()), eq("true"))).thenReturn(new V1ReplicationControllerBuilder()
                .withStatus(new V1ReplicationControllerStatusBuilder().withReplicas(1).withReadyReplicas(0).build())
                .build())
                                                                                                                   .thenReturn(new V1ReplicationControllerBuilder()
                                                                                                                           .withStatus(new V1ReplicationControllerStatusBuilder()
                                                                                                                                   .withReplicas(1)
                                                                                                                                   .withReadyReplicas(1)
                                                                                                                                   .build())
                                                                                                                           .build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testPlatformHealthNotAvailable () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new ApiException());
        assertEquals(PlatformHealth.FAILED, platform.getPlatformHealth());
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
                                                                              .withUUID(randomUUID().toString())
                                                                              .withOwnerKind("")
                                                                              .build();
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(list);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                               .get(0)));
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenReturn(pod);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.checkHealth((KubernetesPodContainer) platform.getRoster()
                                                                                                               .get(0)));
    }

    private static V1PodList getV1PodList (boolean isBackedByController, int numberOfPods) {
        List<V1OwnerReference> ownerReferences = new ArrayList<>();
        if (isBackedByController) {
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
    public void testGetNamespace () {
        assertEquals("mynamespace", platform.getNamespace());
    }

    private static V1PodList getV1PodList (boolean isBackedByController) {
        return getV1PodList(isBackedByController, 1);
    }

    @Test
    public void testCheckDesiredReplicasReplicaSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ReplicaSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedReplicaSetStatus(eq("dummy"), eq(pods.getItems()
                                                                          .get(0)
                                                                          .getMetadata()
                                                                          .getNamespace()), eq("true"))).thenReturn(new V1ReplicaSetBuilder()
                .withStatus(new V1ReplicaSetStatusBuilder().withReplicas(1).withReadyReplicas(0).build())
                .build())
                                                                                                        .thenReturn(new V1ReplicaSetBuilder()
                                                                                                                .withStatus(new V1ReplicaSetStatusBuilder()
                                                                                                                        .withReplicas(1)
                                                                                                                        .withReadyReplicas(1)
                                                                                                                        .build())
                                                                                                                .build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasStatefulSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("StatefulSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedStatefulSetStatus(eq("dummy"), eq(pods.getItems()
                                                                           .get(0)
                                                                           .getMetadata()
                                                                           .getNamespace()), eq("true"))).thenReturn(new V1StatefulSetBuilder()
                .withStatus(new V1StatefulSetStatusBuilder().withReplicas(1).withReadyReplicas(0).build())
                .build())
                                                                                                         .thenReturn(new V1StatefulSetBuilder()
                                                                                                                 .withStatus(new V1StatefulSetStatusBuilder()
                                                                                                                         .withReplicas(1)
                                                                                                                         .withReadyReplicas(1)
                                                                                                                         .build())
                                                                                                                 .build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasDeployment () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("Deployment");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDeploymentStatus(eq("dummy"), eq(pods.getItems()
                                                                          .get(0)
                                                                          .getMetadata()
                                                                          .getNamespace()), eq("true"))).thenReturn(new V1DeploymentBuilder()
                .withStatus(new V1DeploymentStatusBuilder().withReplicas(1).withReadyReplicas(0).build())
                .build())
                                                                                                        .thenReturn(new V1DeploymentBuilder()
                                                                                                                .withStatus(new V1DeploymentStatusBuilder()
                                                                                                                        .withReplicas(1)
                                                                                                                        .withReadyReplicas(1)
                                                                                                                        .build())
                                                                                                                .build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasDaemonSet () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("DaemonSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDaemonSetStatus(eq("dummy"), eq(pods.getItems()
                                                                         .get(0)
                                                                         .getMetadata()
                                                                         .getNamespace()), eq("true"))).thenReturn(new V1DaemonSetBuilder()
                .withStatus(new V1DaemonSetStatusBuilder().withDesiredNumberScheduled(1)
                                                          .withCurrentNumberScheduled(0)
                                                          .build())
                .build())
                                                                                                       .thenReturn(new V1DaemonSetBuilder()
                                                                                                               .withStatus(new V1DaemonSetStatusBuilder()
                                                                                                                       .withDesiredNumberScheduled(1)
                                                                                                                       .withCurrentNumberScheduled(1)
                                                                                                                       .build())
                                                                                                               .build());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
        assertTrue(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasJob () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("ControllerKind");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasException () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("DaemonSet");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(appsV1Api.readNamespacedDaemonSetStatus(eq("dummy"), eq(pods.getItems()
                                                                         .get(0)
                                                                         .getMetadata()
                                                                         .getNamespace()), eq("true"))).thenThrow(new ApiException());
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasCronJob () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("CronJob");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void testCheckDesiredReplicasUnsupportedKind () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("CronJob");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setKind("Unsupported");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        assertFalse(platform.isDesiredReplicas((KubernetesPodContainer) platform.getRoster().get(0)));
    }

    @Test
    public void replicaSetRecovered () throws ApiException {
        V1PodList pods = getV1PodList(randomUUID().toString());
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        pods.getItems()
            .get(0)
            .getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"), eq(pods.getItems()
                                                                                     .get(0)
                                                                                     .getMetadata()
                                                                                     .getNamespace()), eq("true"))).thenReturn(new V1ReplicationControllerBuilder()
                .withStatus(new V1ReplicationControllerStatusBuilder().withReplicas(1).withReadyReplicas(1).build())
                .build());
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
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems()
            .get(0)
            .getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"), eq(pods.getItems()
                                                                                     .get(0)
                                                                                     .getMetadata()
                                                                                     .getNamespace()), eq("true"))).thenReturn(new V1ReplicationControllerBuilder()
                .withStatus(new V1ReplicationControllerStatusBuilder().withReplicas(1).withReadyReplicas(1).build())
                .build());
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pods.getItems().get(0));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
    }

    @Test
    public void replicaSetRecoveredNotInDesiredState () throws ApiException {
        V1PodList pods = getV1PodList(true);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(pods);
        pods.getItems()
            .get(0)
            .getMetadata().getOwnerReferences().get(0).setKind("ReplicationController");
        pods.getItems().get(0).getMetadata().getOwnerReferences().get(0).setName("dummy");
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"), eq(pods.getItems()
                                                                                     .get(0)
                                                                                     .getMetadata()
                                                                                     .getNamespace()), eq("true"))).thenReturn(new V1ReplicationControllerBuilder()
                .withStatus(new V1ReplicationControllerStatusBuilder().withReplicas(1).withReadyReplicas(0).build())
                .build());
        KubernetesPodContainer kubernetesPodContainer = platform.fromKubernetesAPIPod(pods.getItems().get(0));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
        when(coreV1Api.readNamespacedReplicationControllerStatus(eq("dummy"), eq(pods.getItems()
                                                                                     .get(0)
                                                                                     .getMetadata()
                                                                                     .getNamespace()), eq("true"))).thenReturn(new V1ReplicationControllerBuilder()
                .withStatus(new V1ReplicationControllerStatusBuilder().withReplicas(2).withReadyReplicas(1).build())
                .build());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, platform.replicaSetRecovered(kubernetesPodContainer));
    }

    @Test
    public void checkHealthNullPointerExceptionTest () throws Exception {
        final String podName = randomUUID().toString();
        final String namespace = randomUUID().toString();
        final KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        doReturn(UUID).when(kubernetesPodContainer).getUUID();
        doReturn(podName).when(kubernetesPodContainer).getPodName();
        doReturn(namespace).when(kubernetesPodContainer).getNamespace();
        final V1Pod v1Pod = mock(V1Pod.class);
        final V1PodStatus v1PodStatus = mock(V1PodStatus.class);
        final V1ContainerStatus v1ContainerStatus = mock(V1ContainerStatus.class);
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(UUID));
        doReturn(null, v1Pod).when(coreV1Api).readNamespacedPodStatus(podName, namespace, "true");
        doReturn(null, v1PodStatus).when(v1Pod).getStatus();
        doReturn(null, Collections.singletonList(v1ContainerStatus)).when(v1PodStatus).getContainerStatuses();
        doReturn(false, true).when(v1ContainerStatus).isReady();
        assertEquals("Null pod", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
        assertEquals("Null pod status", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
        assertEquals("Null container statuses", ContainerHealth.DOES_NOT_EXIST, platform.checkHealth(kubernetesPodContainer));
        assertEquals("No ready containers", ContainerHealth.RUNNING_EXPERIMENT, platform.checkHealth(kubernetesPodContainer));
        assertEquals("Containers ready", ContainerHealth.NORMAL, platform.checkHealth(kubernetesPodContainer));
    }

    @Test
    public void testIsContainerRecycled () throws ApiException {
        String uid = randomUUID().toString();
        String containerName = randomUUID().toString();
        KubernetesPodContainer kubernetesPodContainer = spy(KubernetesPodContainer.builder()
                                                                                  .withSubcontainers(List.of(containerName))
                                                                                  .build());
        V1ObjectMeta podMetadata = new V1ObjectMeta().uid(uid);
        V1ContainerState state = new V1ContainerState().running(new V1ContainerStateRunning().startedAt(DateTime.now()
                                                                                                                .minusMinutes(1)));
        V1PodStatus podStatus = new V1PodStatus().containerStatuses(List.of(new V1ContainerStatus().name(containerName)
                                                                                                   .state(state)));
        V1Pod pod = new V1Pod().metadata(podMetadata).status(podStatus);
        doReturn(pod).when(coreV1Api).readNamespacedPodStatus(any(), any(), any());
        doReturn(Instant.now().minusSeconds(100)).when(kubernetesPodContainer).getExperimentStartTime();
        assertTrue("Restarted", platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test
    public void testContainerIsNotRecycledYet () throws ApiException {
        String uid = randomUUID().toString();
        String containerName = randomUUID().toString();
        KubernetesPodContainer kubernetesPodContainer = spy(KubernetesPodContainer.builder()
                                                                                  .withSubcontainers(List.of(containerName))
                                                                                  .build());
        V1ObjectMeta podMetadata = new V1ObjectMeta().uid(uid);
        V1ContainerState state = new V1ContainerState().running(new V1ContainerStateRunning().startedAt(DateTime.now()
                                                                                                                .minusMinutes(10)));
        V1PodStatus podStatus = new V1PodStatus().containerStatuses(List.of(new V1ContainerStatus().name(containerName)
                                                                                                   .state(state)));
        V1Pod pod = new V1Pod().metadata(podMetadata).status(podStatus);
        doReturn(pod).when(coreV1Api).readNamespacedPodStatus(any(), any(), any());
        doReturn(Instant.now().minusSeconds(60)).when(kubernetesPodContainer).getExperimentStartTime();
        assertFalse("Not restarted", platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test
    public void testIsContainerRecycledNotFound () throws ApiException {
        KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        V1ReplicaSet replicaSet = new V1ReplicaSetBuilder().withStatus(new V1ReplicaSetStatusBuilder().withReplicas(1)
                                                                                                      .withReadyReplicas(1)
                                                                                                      .build()).build();
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(new V1PodList());
        when(kubernetesPodContainer.getOwnerKind()).thenReturn(ControllerKind.REPLICA_SET);
        when(appsV1Api.readNamespacedReplicaSetStatus(any(), any(), any())).thenReturn(replicaSet);
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenThrow(new ApiException(KubernetesConstants.KUBERNETES_POD_NOT_FOUND_ERROR_MESSAGE));
        assertEquals("Restarted", true, platform.isContainerRecycled(kubernetesPodContainer));
    }

    @Test(expected = ChaosException.class)
    public void testIsContainerRecycledAPIError () throws ApiException {
        KubernetesPodContainer kubernetesPodContainer = mock(KubernetesPodContainer.class);
        V1ReplicaSet replicaSet = new V1ReplicaSetBuilder().withStatus(new V1ReplicaSetStatusBuilder().withReplicas(1)
                                                                                                      .withReadyReplicas(1)
                                                                                                      .build()).build();
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(new V1PodList());
        when(kubernetesPodContainer.getOwnerKind()).thenReturn(ControllerKind.REPLICA_SET);
        when(appsV1Api.readNamespacedReplicaSetStatus(any(), any(), any())).thenReturn(replicaSet);
        when(coreV1Api.readNamespacedPodStatus(any(), any(), any())).thenThrow(new ApiException("ERROR"));
        platform.isContainerRecycled(kubernetesPodContainer);
    }

    @Test
    public void testRecycleContainer () throws ApiException {
        when(coreV1Api.listNamespacedPod(anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(getV1PodList(true));
        KubernetesPodContainer container = mock(KubernetesPodContainer.class);
        platform.recycleContainer(container);
        verify(platform, times(1)).deletePod(container);
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private ContainerManager containerManager;
        @MockBean
        private CoreApi coreApi;
        @MockBean
        private CoreV1Api coreV1Api;
        @MockBean
        private Exec exec;
        @MockBean
        private AppsV1Api appsV1Api;

        @Bean
        KubernetesPlatform kubernetesPlatform () {
            KubernetesPlatform platform = new KubernetesPlatform(coreApi, coreV1Api, exec, appsV1Api);
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