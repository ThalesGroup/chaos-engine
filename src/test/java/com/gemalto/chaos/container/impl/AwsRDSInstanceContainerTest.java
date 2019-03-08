package com.gemalto.chaos.container.impl;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;
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

    @Before
    public void setUp () {
        awsRDSInstanceContainer = AwsRDSInstanceContainer.builder()
                                                         .withEngine(engine)
                                                         .withDbInstanceIdentifier(dbInstanceIdentifier)
                                                         .withAwsRDSPlatform(awsRDSPlatform)
                                                         .withDbiResourceId(DBI_RESOURCE_ID)
                                                         .build();
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
        Experiment experiment = mock(Experiment.class);
        awsRDSInstanceContainer.restartInstance(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(any());
        verify(awsRDSPlatform, times(1)).restartInstance(dbInstanceIdentifier);
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier()
                .withKey("dbinstanceidentifier")
                .withValue(dbInstanceIdentifier), awsRDSInstanceContainer.getDataDogIdentifier());
    }

    @Test
    public void startSnapshot () throws Exception {
        Experiment experiment = spy(Experiment.class);
        DBSnapshot dbSnapshot = mock(DBSnapshot.class);
        doReturn(dbSnapshot).when(awsRDSPlatform).snapshotDBInstance(dbInstanceIdentifier);
        awsRDSInstanceContainer.startSnapshot(experiment);
        verify(awsRDSPlatform, times(1)).snapshotDBInstance(dbInstanceIdentifier);
        verify(experiment, times(1)).setFinalizeMethod(any());
        verify(experiment, times(1)).setSelfHealingMethod(any());
        verify(experiment, times(1)).setCheckContainerHealth(any());
        experiment.getSelfHealingMethod().call();
        verify(awsRDSPlatform, times(1)).deleteInstanceSnapshot(dbSnapshot);
        reset(awsRDSPlatform);
        experiment.getFinalizeMethod().call();
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