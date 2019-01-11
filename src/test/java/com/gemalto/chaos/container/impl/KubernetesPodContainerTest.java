package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.impl.KubernetesPlatform;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1ObjectMetaBuilder;
import io.kubernetes.client.models.V1OwnerReferenceBuilder;
import io.kubernetes.client.models.V1Pod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class KubernetesPodContainerTest {
    private static final String NAME = randomUUID().toString();
    private static final String NAMESPACE_NAME = randomUUID().toString();
    private static final Map<String, String> LABELS = Collections.emptyMap();
    private KubernetesPodContainer kubernetesPodContainer;
    @MockBean
    private KubernetesPlatform kubernetesPlatform;
    @Spy
    private Experiment experiment = new Experiment() {
    };

    @Before
    public void setUp () {
        kubernetesPodContainer = Mockito.spy(KubernetesPodContainer.builder()
                                                                   .withPodName(NAME)
                                                                   .withNamespace(NAMESPACE_NAME)
                                                                   .withKubernetesPlatform(kubernetesPlatform)
                                                                   .withLabels(LABELS)
                                                                   .build());
    }

    @Test
    public void getPlatform () {
        assertEquals(kubernetesPlatform, kubernetesPodContainer.getPlatform());
    }

    @Test
    public void stopContainer () throws Exception {
        kubernetesPodContainer.stopContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(kubernetesPlatform, times(1)).stopInstance(ArgumentMatchers.any());
    }

    @Test
    public void testCreationFromAPI () throws Exception {
        KubernetesPodContainer container = KubernetesPodContainer.fromKubernetesAPIPod(getV1Pod(true), kubernetesPlatform);
        assertEquals(NAME, container.getPodName());
        assertEquals(NAMESPACE_NAME, container.getNamespace());
    }

    private static final V1Pod getV1Pod (boolean isBackedByController) {
        List ownerReferences = new ArrayList<>();
        if (isBackedByController) {
            ownerReferences.add(new V1OwnerReferenceBuilder().withNewController("mycontroller").build());
        }
        V1ObjectMeta metadata = new V1ObjectMetaBuilder().withName(NAME)
                                                         .withNamespace(NAMESPACE_NAME)
                                                         .withLabels(new HashMap<>())
                                                         .withOwnerReferences(ownerReferences)
                                                         .build();
        V1Pod pod = new V1Pod();
        pod.setMetadata(metadata);
        return pod;
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s)", NAME, NAMESPACE_NAME);
        assertEquals(EXPECTED_NAME, kubernetesPodContainer.getSimpleName());
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier()
                                      .withValue(NAME)
                                      .withKey("host"), kubernetesPodContainer.getDataDogIdentifier());
    }

    @Test
    public void dataDogTags () {
        Map<String, String> baseContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        kubernetesPodContainer.setMappedDiagnosticContext();
        Map<String, String> modifiedContextMap = MDC.getCopyOfContextMap();
        kubernetesPodContainer.clearMappedDiagnosticContext();
        Map<String, String> finalContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, NAME);
        expectedTags.putAll(baseContextMap);
        assertEquals(baseContextMap, finalContextMap);
        assertEquals(expectedTags, modifiedContextMap);
    }
}