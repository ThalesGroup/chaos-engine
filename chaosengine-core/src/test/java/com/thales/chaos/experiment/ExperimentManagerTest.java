package com.thales.chaos.experiment;

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.enums.ExperimentState;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.PlatformManager;
import com.thales.chaos.scripts.ScriptManager;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.thales.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    @MockBean
    private ScriptManager scriptManager;
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
    @MockBean(name = "firstPlatform")
    private Platform firstPlatform;
    @MockBean(name = "secondPlatform")
    private Platform secondPlatform;

    @Before
    public void setUp () throws Exception {
        doCallRealMethod().when(platformManager).getNextPlatformForExperiment(anyBoolean());
        doReturn(true).when(platform).hasEligibleContainersForExperiments();
        doReturn(dataDogIdentifier()).when(container1).getDataDogIdentifier();
        doReturn(dataDogIdentifier()).when(container2).getDataDogIdentifier();
        doReturn(dataDogIdentifier()).when(container3).getDataDogIdentifier();
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(secondPlatform).when(secondPlatform).scheduleExperiment();
    }

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
        Platform mockPlatform = mock(Platform.class);
        List<Container> containerListApps = new ArrayList<>();
        containerListApps.add(container1);
        containerListApps.add(container2);
        List<Platform> platforms = new ArrayList<>();
        platforms.add(mockPlatform);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(mockPlatform.scheduleExperiment()).thenReturn(mockPlatform);
        when(mockPlatform.getRoster()).thenReturn(containerListApps);
        when(mockPlatform.generateExperimentRoster()).thenCallRealMethod();
        when(mockPlatform.canExperiment()).thenReturn(true);
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container1.getPlatform()).thenReturn(mockPlatform);
        when(container2.canExperiment()).thenReturn(true);
        when(container2.createExperiment()).thenReturn(experiment2);
        when(container2.getPlatform()).thenReturn(mockPlatform);
        when(experiment1.startExperiment()).thenReturn(completedFuture(true));
        when(experiment2.startExperiment()).thenReturn(completedFuture(true));
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
        doReturn(Optional.of(firstPlatform), Optional.of(secondPlatform), Optional.empty()).when(platformManager)
                                                                                           .getNextPlatformForExperiment(anyBoolean());
        doReturn(List.of(container1, container2)).when(firstPlatform).generateExperimentRoster();
        doReturn(List.of(container3)).when(secondPlatform).generateExperimentRoster();
        doReturn(List.of(firstPlatform, secondPlatform)).when(platformManager).getPlatforms();
        doReturn(true).when(container1).canExperiment();
        doReturn(experiment1).when(container1).createExperiment();
        doReturn(firstPlatform).when(container1).getPlatform();
        doReturn(true).when(container2).canExperiment();
        doReturn(experiment2).when(container2).createExperiment();
        doReturn(firstPlatform).when(container2).getPlatform();
        doReturn(true).when(container3).canExperiment();
        doReturn(experiment3).when(container3).createExperiment();
        doReturn(secondPlatform).when(container3).getPlatform();
        doReturn(completedFuture(true)).when(experiment1).startExperiment();
        doReturn(container1).when(experiment1).getContainer();
        doReturn(completedFuture(true)).when(experiment2).startExperiment();
        doReturn(container2).when(experiment2).getContainer();
        doReturn(completedFuture(true)).when(experiment3).startExperiment();
        doReturn(container3).when(experiment3).getContainer();
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
        assertEquals(1, activeExperiments.stream()
                                         .collect(Collectors.groupingBy(experiment -> experiment.getContainer()
                                                                                                .getPlatform()))
                                     .size());
    }

    @Test
    public void removeFinishedExperiments () {
        when(platformManager.getPlatforms()).thenReturn(Collections.singletonList(firstPlatform));
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(anyBoolean());
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(List.of(container1, container2)).when(firstPlatform).generateExperimentRoster();
        when(container1.canExperiment()).thenReturn(true);
        when(container1.createExperiment()).thenReturn(experiment1);
        when(container1.getPlatform()).thenReturn(firstPlatform);
        when(container2.canExperiment()).thenReturn(true);
        when(container2.createExperiment()).thenReturn(experiment2);
        when(container2.getPlatform()).thenReturn(firstPlatform);
        when(experiment1.startExperiment()).thenReturn(completedFuture(true));
        when(experiment2.startExperiment()).thenReturn(completedFuture(true));
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

    @Test
    public void experimentStartFailed () {
        doReturn(false).when(holidayManager).isOutsideWorkingHours();
        doReturn(false).when(holidayManager).isHoliday();
        Experiment experimentA = Mockito.mock(Experiment.class);
        Experiment experimentB = Mockito.mock(Experiment.class);
        experimentManager.addExperiment(experimentA);
        experimentManager.addExperiment(experimentB);
        CompletableFuture<Boolean> failedStartup = new CompletableFuture<>();
        failedStartup.completeExceptionally(new ChaosException("Failed startup"));
        CompletableFuture<Boolean> successfulStartup = new CompletableFuture<>();
        successfulStartup.complete(Boolean.TRUE);
        doReturn(successfulStartup).when(experimentA).startExperiment();
        doReturn(failedStartup).when(experimentB).startExperiment();
        experimentManager.startNewExperiments();
        assertThat(experimentManager.getActiveExperiments(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA));
        verify(experimentB, times(1).description("Expected ExperimentB's getId to be evaluated for logging")).getId();
    }

    @Test
    public void experimentStartInterrupted () throws InterruptedException, ExecutionException {
        doReturn(false).when(holidayManager).isOutsideWorkingHours();
        doReturn(false).when(holidayManager).isHoliday();
        Experiment experimentA = Mockito.mock(Experiment.class);
        Experiment experimentB = Mockito.mock(Experiment.class);
        experimentManager.addExperiment(experimentA);
        experimentManager.addExperiment(experimentB);
        Future interruptedStartup = mock(Future.class);
        doReturn(true).when(interruptedStartup).isDone();
        doThrow(new InterruptedException()).when(interruptedStartup).get();
        CompletableFuture<Boolean> successfulStartup = new CompletableFuture<>();
        successfulStartup.complete(Boolean.TRUE);
        doReturn(successfulStartup).when(experimentA).startExperiment();
        doReturn(interruptedStartup).when(experimentB).startExperiment();
        experimentManager.startNewExperiments();
        assertThat(experimentManager.getActiveExperiments(), IsIterableContainingInAnyOrder.containsInAnyOrder(experimentA));
        verify(experimentB, times(1).description("Expected ExperimentB's getId to be evaluated for logging")).getId();
        assertTrue(Thread.interrupted());
    }

    @Configuration
    static class ExperimentManagerTestConfiguration {
        @Autowired
        private NotificationManager notificationManager;
        @Autowired
        private PlatformManager platformManager;
        @Autowired
        private HolidayManager holidayManager;
        @Autowired
        private Platform firstPlatform;
        @Autowired
        private Platform secondPlatform;

        @Bean
        Platform firstPlatform () {
            return mock(Platform.class);
        }

        @Bean
        Platform secondPlatform () {
            return mock(Platform.class);
        }

        @Bean
        ExperimentManager experimentManager () {
            return Mockito.spy(new ExperimentManager(platformManager, holidayManager));
        }
    }
}