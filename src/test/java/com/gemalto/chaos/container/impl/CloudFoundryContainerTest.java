package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;
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
    @Spy
    private Experiment experiment = new Experiment() {
    };
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
    }

    @Test
    public void supportsShellBasedExperiments () {
//        assertTrue(cloudFoundryContainer.supportsShellBasedExperiments());
    }

    @Test
    public void getIdentity () {
        cloudFoundryContainer = new CloudFoundryContainer("AppID", "ApplicationEngine", 1);
        CRC32 checksum = new CRC32();
        checksum.update("AppID$$$$$ApplicationEngine$$$$$1".getBytes());
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
    public void createExperiment () {
        Experiment experiment = cloudFoundryContainer.createExperiment(ExperimentType.STATE);
        assertEquals(cloudFoundryContainer, experiment.getContainer());
        assertEquals(ExperimentType.STATE, experiment.getExperimentType());
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
        assertEquals(dataDogIdentifier().withKey("host")
                                        .withValue(name + "-" + instance), cloudFoundryContainer.getDataDogIdentifier());
    }

    @Test
    public void supportShellBasedExperiments () {
        assertTrue(cloudFoundryContainer.supportsShellBasedExperiments());
    }
}