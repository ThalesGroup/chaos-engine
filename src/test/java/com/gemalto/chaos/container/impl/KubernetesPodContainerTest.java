package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.impl.KubernetesPlatform;
import com.gemalto.chaos.ssh.impl.experiments.ForkBomb;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void stopContainer () {
        kubernetesPodContainer.stopContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(kubernetesPlatform, times(1)).stopInstance(ArgumentMatchers.any());
    }

    @Test
    public void forkBomb () {
        kubernetesPodContainer.forkBomb(experiment);
        Mockito.verify(kubernetesPlatform, times(1)).sshExperiment(any(ForkBomb.class), eq(kubernetesPodContainer));
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s (%s)", NAME, NAMESPACE_NAME);
        assertEquals(EXPECTED_NAME, kubernetesPodContainer.getSimpleName());
        assertEquals(NAME, kubernetesPodContainer.getPodName());
        assertEquals(NAMESPACE_NAME, kubernetesPodContainer.getNamespace());
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