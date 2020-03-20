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

package com.thales.chaos.container.impl;

import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.enums.ControllerKind;
import com.thales.chaos.platform.impl.KubernetesPlatform;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class KubernetesPodContainerTest {
    private static final String NAME = randomUUID().toString();
    private static final String NAMESPACE_NAME = randomUUID().toString();
    private static final String OWNER_NAME = randomUUID().toString();
    private static final String OWNER_KIND = "Deployment";
    private static final Map<String, String> LABELS = Collections.emptyMap();
    private KubernetesPodContainer kubernetesPodContainer;
    @MockBean
    private KubernetesPlatform kubernetesPlatform;
    @Mock
    private Experiment experiment;

    @Before
    public void setUp () {
        kubernetesPodContainer = KubernetesPodContainer.builder()
                                                       .withPodName(NAME)
                                                       .withNamespace(NAMESPACE_NAME)
                                                       .withKubernetesPlatform(kubernetesPlatform)
                                                       .withOwnerKind(OWNER_KIND)
                                                       .withOwnerName(OWNER_NAME)
                                                       .withLabels(LABELS)
                                                       .isBackedByController(true)
                                                       .build();
        when(experiment.getContainer()).thenReturn(kubernetesPodContainer);
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getCheckContainerHealth();
            return null;
        }).when(experiment).setCheckContainerHealth(any());
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getSelfHealingMethod();
            return null;
        }).when(experiment).setSelfHealingMethod(any());
        doAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            doReturn(args[0]).when(experiment).getFinalizeMethod();
            return null;
        }).when(experiment).setFinalizeMethod(any());
    }

    @Test
    public void getAggegationIdentifier () {
        assertEquals(OWNER_NAME, kubernetesPodContainer.getAggregationIdentifier());
    }

    @Test
    public void getPlatform () {
        assertEquals(kubernetesPlatform, kubernetesPodContainer.getPlatform());
    }

    @Test
    public void testGetOwnerName () {
        assertEquals(OWNER_NAME, kubernetesPodContainer.getOwnerName());
    }

    @Test
    public void testGetOwnerKind () {
        TestCase.assertEquals(ControllerKind.DEPLOYMENT, kubernetesPodContainer.getOwnerKind());
    }

    @Test
    public void testCanExperiment () {
        assertTrue(kubernetesPodContainer.canExperiment() || !kubernetesPodContainer.canExperiment());
    }

    @Test
    public void testUniqueIdentifierInner () {
        assertTrue(kubernetesPodContainer.compareUniqueIdentifierInner(kubernetesPodContainer.getPodName()));
    }

    @Test
    public void updateContainerHealthImpl () {
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            when(kubernetesPlatform.checkHealth(eq(kubernetesPodContainer))).thenReturn(containerHealth);
            assertEquals(containerHealth, kubernetesPodContainer.updateContainerHealthImpl(experiment.getExperimentType()));
        }
    }

    @Test
    public void deletePod () throws Exception {
        kubernetesPodContainer.deletePod(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, never()).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(kubernetesPlatform, times(1)).deletePod(kubernetesPodContainer);
        experiment.getCheckContainerHealth().call();
        Mockito.verify(kubernetesPlatform, times(1)).replicaSetRecovered(kubernetesPodContainer);
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
        TestCase.assertEquals(DataDogIdentifier.dataDogIdentifier().withValue(NAME).withKey("host"), kubernetesPodContainer.getDataDogIdentifier());
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