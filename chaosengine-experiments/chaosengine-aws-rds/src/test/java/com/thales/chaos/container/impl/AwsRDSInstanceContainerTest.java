/*
 *    Copyright (c) 2019 Thales Group
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

import com.amazonaws.services.rds.model.DBSnapshot;
import com.thales.chaos.constants.AwsRDSConstants;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.impl.AwsRDSPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AwsRDSInstanceContainerTest {
    private static final String DBI_RESOURCE_ID = UUID.randomUUID().toString();
    @MockBean
    private AwsRDSPlatform awsRDSPlatform;
    private AwsRDSInstanceContainer awsRDSInstanceContainer;
    private String dbInstanceIdentifier = UUID.randomUUID().toString();
    private String engine = UUID.randomUUID().toString();
    private Experiment experiment = mock(Experiment.class);

    @Before
    public void setUp () {
        awsRDSInstanceContainer = AwsRDSInstanceContainer.builder()
                                                         .withEngine(engine)
                                                         .withDbInstanceIdentifier(dbInstanceIdentifier)
                                                         .withAwsRDSPlatform(awsRDSPlatform)
                                                         .withDbiResourceId(DBI_RESOURCE_ID)
                                                         .build();
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
        assertEquals(dbInstanceIdentifier, awsRDSInstanceContainer.getAggregationIdentifier());
    }

    @Test
    public void getDbInstanceIdentifier () {
        assertEquals(dbInstanceIdentifier, awsRDSInstanceContainer.getDbInstanceIdentifier());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsRDSPlatform, awsRDSInstanceContainer.getPlatform());
    }

    @Test
    public void supportsShellBasedExperiments () {
        assertFalse(awsRDSInstanceContainer.supportsShellBasedExperiments());
    }

    @Test
    public void getSimpleName () {
        assertEquals(dbInstanceIdentifier, awsRDSInstanceContainer.getSimpleName());
    }

    @Test
    public void restartInstance () {
        awsRDSInstanceContainer.restartInstance(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(any());
        verify(awsRDSPlatform, times(1)).restartInstance(dbInstanceIdentifier);
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier().withKey("dbinstanceidentifier").withValue(dbInstanceIdentifier), awsRDSInstanceContainer.getDataDogIdentifier());
    }

    @Test
    public void startSnapshot () throws Exception {
        DBSnapshot dbSnapshot = mock(DBSnapshot.class);
        doReturn(dbSnapshot).when(awsRDSPlatform).snapshotDBInstance(dbInstanceIdentifier);
        awsRDSInstanceContainer.startSnapshot(experiment);
        verify(awsRDSPlatform, times(1)).snapshotDBInstance(dbInstanceIdentifier);
        verify(experiment, times(1)).setFinalizeMethod(any());
        verify(experiment, times(1)).setSelfHealingMethod(any());
        verify(experiment, times(1)).setCheckContainerHealth(any());
        experiment.getSelfHealingMethod().run();
        verify(awsRDSPlatform, times(1)).deleteInstanceSnapshot(dbSnapshot);
        reset(awsRDSPlatform);
        experiment.getFinalizeMethod().run();
        verify(awsRDSPlatform, times(1)).deleteInstanceSnapshot(dbSnapshot);
        reset(awsRDSPlatform);
        doReturn(true, false).when(awsRDSPlatform).isInstanceSnapshotRunning(dbInstanceIdentifier);
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, experiment.getCheckContainerHealth().call());
        assertEquals(ContainerHealth.NORMAL, experiment.getCheckContainerHealth().call());
    }

    @Test
    public void dataDogTags () {
        Map<String, String> baseContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        awsRDSInstanceContainer.setMappedDiagnosticContext();
        Map<String, String> modifiedContextMap = MDC.getCopyOfContextMap();
        awsRDSInstanceContainer.clearMappedDiagnosticContext();
        Map<String, String> finalContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, DBI_RESOURCE_ID);
        expectedTags.put(AwsRDSConstants.AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier);
        expectedTags.putAll(baseContextMap);
        assertEquals(baseContextMap, finalContextMap);
        assertEquals(expectedTags, modifiedContextMap);
    }
}