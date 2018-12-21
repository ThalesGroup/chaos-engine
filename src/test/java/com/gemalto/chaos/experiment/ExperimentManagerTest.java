package com.gemalto.chaos.experiment;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentState;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ExperimentManagerTest {
    @Autowired
    private ExperimentManager experimentManager;
    @MockBean
    private NotificationManager notificationManager;
    @MockBean
    private PlatformManager platformManager;
    @MockBean
    private HolidayManager holidayManager;
    @Mock
    private Experiment experiment1;
    @Mock
    private Experiment experiment2;
    @Mock
    private Experiment experiment3;
    @Mock
    private Container container1;
    @Mock
    private Container container2;
    @Mock
    private Container container3;
    @Mock
    private Platform platform;
    @MockBean
    private AdminManager adminManager;
    @MockBean
    private AwsRDSPlatform awsRDSPlatform;
    @MockBean
    private AwsEC2Platform awsEC2Platform;
    @MockBean
    private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    @MockBean
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;


    @Test
    public void startExperiments () {
        List<Container> containerList = new ArrayList<>();
        containerList.add(container1);
        containerList.add(container2);
        when(platformManager.getPlatforms()).thenReturn(Collections.singleton(platform));
        when(platform.scheduleExperiment()).thenReturn(platform);
        when(platform.generateExperimentRoster()).thenCallRealMethod();
        when(platform.getRoster()).thenReturn(containerList);
        when(platform.canExperiment()).thenReturn(true);
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container2.canExperiment()).thenReturn(false);
        experimentManager.scheduleExperiments();
        assertThat(experimentManager.getNewExperimentQueue(), hasItem(experiment1));
        verify(container2, times(1)).canExperiment();
        verify(container2, times(0)).createExperiment();
    }

    //SCT-6233
    public void noExperimentsOnHolidays () {
        CloudFoundryApplicationPlatform pcfApplicationPlatform = mock(CloudFoundryApplicationPlatform.class);
        List<Container> containerListApps = new ArrayList<>();
        containerListApps.add(container1);
        containerListApps.add(container2);
        List<Platform> platforms = new ArrayList<>();
        platforms.add(pcfApplicationPlatform);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(pcfApplicationPlatform.scheduleExperiment()).thenReturn(pcfApplicationPlatform);
        when(pcfApplicationPlatform.getRoster()).thenReturn(containerListApps);
        when(pcfApplicationPlatform.generateExperimentRoster()).thenCallRealMethod();
        when(pcfApplicationPlatform.canExperiment()).thenReturn(true);
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container1.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(container2.canExperiment()).thenReturn(true);
        when(container2.createExperiment()).thenReturn(experiment2);
        when(container2.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(experiment1.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment2.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment1.getContainer()).thenReturn(container1);
        when(experiment2.getContainer()).thenReturn(container2);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isOutsideWorkingHours()).thenReturn(true);
        experimentManager.scheduleExperiments();
        Collection<Experiment> experiments = experimentManager.getNewExperimentQueue();
        experimentManager.updateExperimentStatus();
        Collection<Experiment> activeExperiments = experimentManager.getActiveExperiments();
        int activeExperimentsCount = activeExperiments.size();
        assertEquals(0, activeExperimentsCount);
    }

    //SCT-5854
    @Test
    public void avoidOverlappingExperiments () {
        List<Container> containerListApps = new ArrayList<>();
        containerListApps.add(container1);
        containerListApps.add(container2);
        List<Container> containerListContainers = new ArrayList<>();
        containerListContainers.add(container3);
        doReturn(dataDogIdentifier()).when(container1).getDataDogIdentifier();
        doReturn(dataDogIdentifier()).when(container2).getDataDogIdentifier();
        doReturn(dataDogIdentifier()).when(container3).getDataDogIdentifier();
        when(platformManager.getPlatforms()).thenReturn(Arrays.asList(cloudFoundryApplicationPlatform, cloudFoundryContainerPlatform));
        when(cloudFoundryApplicationPlatform.scheduleExperiment()).thenReturn(cloudFoundryApplicationPlatform);
        when(cloudFoundryApplicationPlatform.getRoster()).thenReturn(containerListApps);
        when(cloudFoundryApplicationPlatform.generateExperimentRoster()).thenCallRealMethod();
        when(cloudFoundryApplicationPlatform.canExperiment()).thenReturn(true);
        when(cloudFoundryApplicationPlatform.getNextChaosTime()).thenReturn(Instant.now());
        when(cloudFoundryContainerPlatform.scheduleExperiment()).thenReturn(cloudFoundryContainerPlatform);
        when(cloudFoundryContainerPlatform.getRoster()).thenReturn(containerListContainers);
        when(cloudFoundryContainerPlatform.canExperiment()).thenReturn(true);
        when(cloudFoundryContainerPlatform.generateExperimentRoster()).thenCallRealMethod();
        when(cloudFoundryContainerPlatform.getNextChaosTime()).thenReturn(Instant.now());
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container1.getPlatform()).thenReturn(cloudFoundryApplicationPlatform);
        when(container2.canExperiment()).thenReturn(true);
        when(container2.createExperiment()).thenReturn(experiment2);
        when(container2.getPlatform()).thenReturn(cloudFoundryApplicationPlatform);
        when(container3.canExperiment()).thenReturn(true);
        when(container3.createExperiment()).thenReturn(experiment3);
        when(container3.getPlatform()).thenReturn(cloudFoundryContainerPlatform);
        when(experiment1.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment2.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment3.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment1.getContainer()).thenReturn(container1);
        when(experiment2.getContainer()).thenReturn(container2);
        when(experiment3.getContainer()).thenReturn(container3);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isOutsideWorkingHours()).thenReturn(false);
        experimentManager.scheduleExperiments();
        Collection<Experiment> experiments = experimentManager.getNewExperimentQueue();
        int scheduledExperimentsCount = experiments.size();
        experimentManager.scheduleExperiments();
        Collection<Experiment> experiments2 = experimentManager.getNewExperimentQueue();
        // new scheduleExperiments invocation should not add new experiment until newExperimentQueue is empty
        assertEquals(experiments, experiments2);
        experimentManager.updateExperimentStatus();
        Collection<Experiment> activeExperiments = experimentManager.getActiveExperiments();
        int activeExperimentsCount = activeExperiments.size();
        //number active experiments should be equal to number of previously scheduled experiments
        assertEquals(scheduledExperimentsCount, activeExperimentsCount);
        //all active experiments should belong to same platform layer
        Platform experimentPlatform = activeExperiments.iterator().next().getContainer().getPlatform();
        assertEquals(1, activeExperiments.stream()
                                         .collect(Collectors.groupingBy(experiment -> experiment.getContainer()
                                                                                                .getPlatform()))
                                     .size());
    }

    @Test
    public void removeFinishedExperiments () {
        doReturn(dataDogIdentifier()).when(container1).getDataDogIdentifier();
        doReturn(dataDogIdentifier()).when(container2).getDataDogIdentifier();
        when(platformManager.getPlatforms()).thenReturn(Collections.singletonList(cloudFoundryApplicationPlatform));
        when(cloudFoundryApplicationPlatform.scheduleExperiment()).thenReturn(cloudFoundryApplicationPlatform);
        when(cloudFoundryApplicationPlatform.getRoster()).thenReturn(Arrays.asList(container1, container2));
        when(cloudFoundryApplicationPlatform.canExperiment()).thenReturn(true);
        when(cloudFoundryApplicationPlatform.generateExperimentRoster()).thenCallRealMethod();
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container1.getPlatform()).thenReturn(cloudFoundryApplicationPlatform);
        when(container2.canExperiment()).thenReturn(true);
        when(container2.createExperiment()).thenReturn(experiment2);
        when(container2.getPlatform()).thenReturn(cloudFoundryApplicationPlatform);
        when(experiment1.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment2.startExperiment()).thenReturn(CompletableFuture.completedFuture(true));
        when(experiment1.getContainer()).thenReturn(container1);
        when(experiment2.getContainer()).thenReturn(container2);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isOutsideWorkingHours()).thenReturn(false);
        //schedule experiments
        experimentManager.scheduleExperiments();
        experimentManager.updateExperimentStatus();
        //check they are active
        assertEquals(2, experimentManager.getActiveExperiments().size());
        when(experiment1.getExperimentState()).thenReturn(ExperimentState.FINISHED);
        when(experiment2.getExperimentState()).thenReturn(ExperimentState.NOT_YET_STARTED);
        //one experiment to be removed
        experimentManager.updateExperimentStatus();
        assertEquals(1, experimentManager.getActiveExperiments().size());
        when(experiment2.getExperimentState()).thenReturn(ExperimentState.FINISHED);
        experimentManager.updateExperimentStatus();
        //all experiments should be removed
        assertEquals(0, experimentManager.getActiveExperiments().size());
    }


    @Test
    public void experimentContainerId () {
        Long containerId = new Random().nextLong();
        Collection<Platform> platforms = Collections.singleton(platform);
        List<Container> roster = new ArrayList<>();
        roster.add(container1);
        roster.add(container2);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(platform.getRoster()).thenReturn(roster);
        when(container1.getIdentity()).thenReturn(containerId);
        when(container2.getIdentity()).thenReturn(containerId + 1);
        doReturn(experiment1).when(container1).createExperiment();
        assertThat(experimentManager.experimentContainerId(containerId), containsInAnyOrder(experiment1));
        verify(container1, times(1)).createExperiment();
        verify(container2, times(0)).createExperiment();
    }

    @Test
    public void testPlatformsByOrder () {
        Platform platform1 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform2 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform3 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform4 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        doReturn(Arrays.asList(platform1, platform2, platform3, platform4)).when(platformManager).getPlatforms();
        doReturn(true).when(platform1).canExperiment();
        doReturn(true).when(platform2).canExperiment();
        doReturn(true).when(platform3).canExperiment();
        doReturn(false).when(platform4).canExperiment();
        doReturn(Collections.singletonList(container1)).when(platform1).getRoster();
        doReturn(Collections.singletonList(container2)).when(platform2).getRoster();
        doReturn(Collections.emptyList()).when(platform3).getRoster();
        doReturn(Instant.now()).when(platform1).getNextChaosTime();
        doReturn(Instant.now().plusSeconds(1)).when(platform2).getNextChaosTime();
        experimentManager.scheduleExperiments(false);
        verify(platform4, never()).getRoster();
        verify(platform1, times(1)).getNextChaosTime();
        verify(platform2, times(1)).getNextChaosTime();
        verify(platform3, times(0)).getNextChaosTime();
        verify(platform4, times(0)).getNextChaosTime();
        verify(platform1, times(1)).scheduleExperiment();
        verify(platform2, never()).scheduleExperiment();
        verify(platform3, never()).scheduleExperiment();
        verify(platform4, never()).scheduleExperiment();
    }

    @Test
    public void testPlatformsByOrderWithNoEligible () {
        Platform platform1 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform2 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform3 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        Platform platform4 = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            protected List<Container> generateRoster () {
                return null;
            }
        });
        doReturn(Arrays.asList(platform1, platform2, platform3, platform4)).when(platformManager).getPlatforms();
        doReturn(true).when(platform1).canExperiment();
        doReturn(true).when(platform2).canExperiment();
        doReturn(false).when(platform3).canExperiment();
        doReturn(false).when(platform4).canExperiment();
        doReturn(Collections.emptyList()).when(platform1).getRoster();
        doReturn(Collections.emptyList()).when(platform2).getRoster();
        assertThat(experimentManager.scheduleExperiments(false), IsEmptyCollection.empty());
        verify(platform3, never()).getRoster();
        verify(platform4, never()).getRoster();
        verify(platform1, never()).getNextChaosTime();
        verify(platform2, never()).getNextChaosTime();
        verify(platform3, never()).getNextChaosTime();
        verify(platform4, never()).getNextChaosTime();
        verify(platform1, never()).scheduleExperiment();
        verify(platform2, never()).scheduleExperiment();
        verify(platform3, never()).scheduleExperiment();
        verify(platform4, never()).scheduleExperiment();
    }

    @Test
    public void experimentsAlwaysReturnedByStartExperiment () {
        doReturn(Collections.singleton(platform)).when(platformManager).getPlatforms();
        doReturn(Collections.singletonList(container1)).when(platform).getRoster();
        doReturn(platform).when(platform).scheduleExperiment();
        doCallRealMethod().when(platform).generateExperimentRoster();
        doReturn(true).when(container1).canExperiment();
        doReturn(experiment1).when(container1).createExperiment();
        doReturn(experiment1).when(experimentManager).addExperiment(experiment1);
        assertThat(experimentManager.scheduleExperiments(true), containsInAnyOrder(experiment1));
    }

    @Test
    public void asynchronousStartExperimentTest () {
        doReturn(false).when(holidayManager).isOutsideWorkingHours();
        doReturn(false).when(holidayManager).isHoliday();
        CompletableFuture<Boolean> experimentAResults = new CompletableFuture<>();
        CompletableFuture<Boolean> experimentBResults = new CompletableFuture<>();
        Experiment experimentA = Mockito.mock(Experiment.class);
        Experiment experimentB = Mockito.mock(Experiment.class);
        doReturn(experimentAResults).when(experimentA).startExperiment();
        doReturn(experimentBResults).when(experimentB).startExperiment();
        experimentManager.addExperiment(experimentA);
        experimentManager.addExperiment(experimentB);
        experimentManager.startNewExperiments();
        verify(experimentA, atLeastOnce()).startExperiment();
        verify(experimentB, atLeastOnce()).startExperiment();
        assertThat(experimentManager.getNewExperimentQueue(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        assertThat(experimentManager.getStartedExperiments()
                                    .keySet(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA, experimentB));
        assertThat(experimentManager.getActiveExperiments(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        experimentManager.transitionExperimentsThatHaveStarted();
        assertThat(experimentManager.getNewExperimentQueue(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        assertThat(experimentManager.getStartedExperiments()
                                    .keySet(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA, experimentB));
        assertThat(experimentManager.getActiveExperiments(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        experimentAResults.complete(true);
        experimentManager.transitionExperimentsThatHaveStarted();
        assertThat(experimentManager.getNewExperimentQueue(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        assertThat(experimentManager.getStartedExperiments()
                                    .keySet(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentB));
        assertThat(experimentManager.getActiveExperiments(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA));
        experimentBResults.complete(false);
        experimentManager.transitionExperimentsThatHaveStarted();
        assertThat(experimentManager.getNewExperimentQueue(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        assertThat(experimentManager.getStartedExperiments()
                                    .keySet(), IsEmptyCollection.emptyCollectionOf(Experiment.class));
        assertThat(experimentManager.getActiveExperiments(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA));
    }


    @Configuration
    static class ExperimentManagerTestConfiguration {
        @Autowired
        private NotificationManager notificationManager;
        @Autowired
        private PlatformManager platformManager;
        @Autowired
        private HolidayManager holidayManager;

        @Bean
        ExperimentManager experimentManager () {
            return Mockito.spy(new ExperimentManager(platformManager, holidayManager));
        }
    }
}