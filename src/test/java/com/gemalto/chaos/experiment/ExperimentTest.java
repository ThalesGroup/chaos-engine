package com.gemalto.chaos.experiment;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.admin.enums.AdminState;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.ResourceExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentState;
import com.gemalto.chaos.experiment.impl.GenericContainerExperiment;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Random;

import static com.gemalto.chaos.experiment.enums.ExperimentType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
public class ExperimentTest {
    private final Duration stateDuration = Duration.ofMinutes(new Random().nextInt(5) + 5);
    private final Duration networkDuration = Duration.ofMinutes(new Random().nextInt(5) + 5);
    private final Duration resourceDuration = Duration.ofMinutes(new Random().nextInt(5) + 5);
    private Experiment stateExperiment;
    private Experiment networkExperiment;
    private Experiment resourceExperiment;
    @Mock
    private StateContainer stateContainer;
    @Mock
    private NetworkContainer networkContainer;
    @Mock
    private ResourceContainer resourceContainer;
    @MockBean
    private NotificationManager notificationManager;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Before
    public void setUp () {
        AdminManager.setAdminState(AdminState.STARTED);

        stateExperiment = Mockito.spy(GenericContainerExperiment.builder()
                                                                .withExperimentType(STATE)
                                                                .withContainer(stateContainer)
                                                                .withDuration(stateDuration)
                                                                .build());
        networkExperiment = Mockito.spy(GenericContainerExperiment.builder()
                                                                  .withExperimentType(NETWORK)
                                                                  .withContainer(networkContainer)
                                                                  .withDuration(networkDuration)
                                                                  .build());
        resourceExperiment = Mockito.spy(GenericContainerExperiment.builder()
                                                                   .withExperimentType(RESOURCE)
                                                                   .withContainer(resourceContainer)
                                                                   .withDuration(resourceDuration)
                                                                   .build());
        autowireCapableBeanFactory.autowireBean(stateExperiment);
        autowireCapableBeanFactory.autowireBean(networkExperiment);
        autowireCapableBeanFactory.autowireBean(resourceExperiment);
    }

    @Test
    public void experimentClassTest () {
        final Platform platform = mock(Platform.class);
        doReturn(ContainerHealth.NORMAL).when(stateContainer).getContainerHealth(STATE);
        doReturn(true).when(stateContainer).supportsExperimentType(STATE);
        doReturn(platform).when(stateContainer).getPlatform();
        assertNull(stateExperiment.getExperimentLayer());
        assertTrue(stateExperiment.startExperiment());
        assertEquals(platform, stateExperiment.getExperimentLayer());
    }

    @Test
    public void cannotStartWhilePaused () {
        AdminManager.setAdminState(AdminState.PAUSED);
        assertFalse(stateExperiment.startExperiment());
        assertFalse(networkExperiment.startExperiment());
        assertFalse(resourceExperiment.startExperiment());
        AdminManager.setAdminState(AdminState.DRAIN);
        assertFalse(stateExperiment.startExperiment());
        assertFalse(networkExperiment.startExperiment());
        assertFalse(resourceExperiment.startExperiment());
    }

    @Test
    public void containerNotStarted () {
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(stateContainer).getContainerHealth(STATE);
        doReturn(ContainerHealth.DOES_NOT_EXIST).when(networkContainer).getContainerHealth(NETWORK);
        doReturn(ContainerHealth.RUNNING_EXPERIMENT).when(resourceContainer).getContainerHealth(RESOURCE);
        assertFalse(stateExperiment.startExperiment());
        assertFalse(networkExperiment.startExperiment());
        assertFalse(resourceExperiment.startExperiment());
    }

    @Test(expected = ChaosException.class)
    public void containerWithNoTestMethods () {
        Container container = mock(Container.class);
        Experiment experiment = Mockito.spy(GenericContainerExperiment.builder()
                                                                      .withExperimentType(STATE)
                                                                      .withContainer(container)
                                                                      .build());
        doReturn(ContainerHealth.NORMAL).when(container).getContainerHealth(STATE);
        doReturn(true).when(container).supportsExperimentType(STATE);
        doReturn(ContainerHealth.NORMAL).when(container).getContainerHealth(STATE);
        experiment.startExperiment();
    }

    @Test
    public void experimentState () {
        stateExperiment.setNotificationManager(notificationManager);
        // No specific health check method, not finalizable
        doReturn(ContainerHealth.NORMAL).when(stateContainer).getContainerHealth(STATE);
        assertEquals(ExperimentState.STARTED, stateExperiment.getExperimentState());
        Mockito.reset(stateContainer);
    }

    private abstract class StateContainer extends Container {
        @StateExperiment
        public abstract void doAThing (Experiment experiment);
    }

    private abstract class NetworkContainer extends Container {
        @NetworkExperiment
        public abstract void doAThing (Experiment experiment);
    }

    private abstract class ResourceContainer extends Container {
        @ResourceExperiment
        public abstract void doAThing (Experiment experiment);
    }
}
