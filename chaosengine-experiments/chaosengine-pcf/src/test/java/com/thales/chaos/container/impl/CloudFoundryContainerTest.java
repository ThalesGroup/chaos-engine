package com.thales.chaos.container.impl;

import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.impl.CloudFoundryContainerPlatform;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryContainerTest {
    private static final String applicationId = UUID.randomUUID().toString();
    private static final int instance = new Random().nextInt(100);
    private static final String name = UUID.randomUUID().toString();
    private CloudFoundryContainer cloudFoundryContainer;
    @Mock
    private Experiment experiment;
    @MockBean
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;

    @Before
    public void setUp () {
        cloudFoundryContainer = CloudFoundryContainer.builder()
                                                     .applicationId(applicationId)
                                                     .instance(instance)
                                                     .name(name)
                                                     .platform(cloudFoundryContainerPlatform)
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
        assertEquals(name, cloudFoundryContainer.getAggregationIdentifier());
    }

    @Test
    public void supportsShellBasedExperiments () {
        assertTrue(cloudFoundryContainer.supportsShellBasedExperiments());
    }

    @Test
    public void getIdentity () {
        cloudFoundryContainer = new CloudFoundryContainer("AppID", "ApplicationEngine", 1);
        CRC32 checksum = new CRC32();
        checksum.update("ApplicationEngine$$$$$1$$$$$AppID".getBytes());
        assertEquals(checksum.getValue(), cloudFoundryContainer.getIdentity());
    }

    @Test
    public void restartContainer () throws Exception {
        cloudFoundryContainer.restartContainer(experiment);
        verify(experiment, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(experiment, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .restartInstance(any(RestartApplicationInstanceRequest.class));
        experiment.getSelfHealingMethod().call();
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s - (%s)", cloudFoundryContainer.getName(), cloudFoundryContainer.getInstance());
        assertEquals(EXPECTED_NAME, cloudFoundryContainer.getSimpleName());
    }

    @Test
    public void getApplicationID () {
        assertEquals(applicationId, cloudFoundryContainer.getApplicationId());
    }

    @Test
    public void getPlatform () {
        assertEquals(cloudFoundryContainerPlatform, cloudFoundryContainer.getPlatform());
    }

    @Test
    public void updateContainerHealthImpl () {
        doReturn(ContainerHealth.NORMAL).when(cloudFoundryContainerPlatform).checkHealth(applicationId, instance);
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainer.updateContainerHealthImpl(ExperimentType.STATE));
    }

    @Test
    public void getDataDogIdentifier () {
        assertEquals(DataDogIdentifier.dataDogIdentifier().withKey("host")
                                      .withValue(name + "-" + instance), cloudFoundryContainer.getDataDogIdentifier());
    }

    @Test
    public void supportShellBasedExperiments () {
        assertTrue(cloudFoundryContainer.supportsShellBasedExperiments());
    }
}