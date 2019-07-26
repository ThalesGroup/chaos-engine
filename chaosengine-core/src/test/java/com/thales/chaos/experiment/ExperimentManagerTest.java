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
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.thales.chaos.exception.enums.ChaosErrorCode.NOT_ENOUGH_CONTAINERS_FOR_PLANNED_EXPERIMENT;
import static com.thales.chaos.exception.enums.ChaosErrorCode.PLATFORM_DOES_NOT_EXIST;
import static com.thales.chaos.experiment.enums.ExperimentState.*;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ExperimentManagerTest {
    @Autowired
    private ExperimentManager experimentManager;
    @MockBean
    private NotificationManager notificationManager;
    @SpyBean
    private PlatformManager platformManager;
    @MockBean
    private HolidayManager holidayManager;
    @MockBean
    private ScriptManager scriptManager;
    @MockBean
    private AdminManager adminManager;
    @MockBean(name = "firstPlatform")
    private Platform firstPlatform;
    @MockBean(name = "secondPlatform")
    private Platform secondPlatform;

    @Before
    public void setUp () {
        doReturn(new ExperimentManager.AutoCloseableMDCCollection(Collections.emptyMap())).when(experimentManager)
                                                                                          .getExperimentAutoCloseableMDCCollection(any());
        doReturn("firstPlatform").when(firstPlatform).getPlatformType();
        doReturn("secondPlatform").when(secondPlatform).getPlatformType();
    }

    @Test
    public void updateExperimentStatus () {
        Experiment firstExperiment = mock(Experiment.class);
        Experiment secondExperiment = mock(Experiment.class);
        when(firstExperiment.getExperimentState()).thenReturn(CREATED);
        when(secondExperiment.getExperimentState()).thenReturn(CREATED);
        experimentManager.addExperiment(firstExperiment);
        experimentManager.addExperiment(secondExperiment);
        experimentManager.updateExperimentStatus();
        verify(experimentManager).evaluateExperiments();
    }

    @Test
    public void updateExperimentStatusWithNoExperiments () {
        experimentManager.updateExperimentStatus();
        verify(experimentManager, never()).evaluateExperiments();
    }

    @Test
    public void updateExperimentStatusAllFinished () {
        Experiment firstExperiment = mock(Experiment.class);
        Experiment secondExperiment = mock(Experiment.class);
        when(firstExperiment.getExperimentState()).thenReturn(FINISHED);
        when(secondExperiment.getExperimentState()).thenReturn(FAILED);
        experimentManager.addExperiment(firstExperiment);
        experimentManager.addExperiment(secondExperiment);
        doReturn(true).when(firstExperiment).isComplete();
        doReturn(true).when(secondExperiment).isComplete();
        experimentManager.updateExperimentStatus();
        verify(experimentManager).evaluateExperiments();
        assertThat(experimentManager.getAllExperiments(), empty());
    }

    @Test
    public void scheduleExperiments () {
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        doReturn(Collections.emptySet()).when(experimentManager).scheduleExperiments(captor.capture());
        experimentManager.scheduleExperiments();
        assertThat(captor.getValue(), is(false));
    }

    @Test
    public void scheduleExperimentsUnforcedWithRoster () {
        Container container = mock(Container.class);
        doReturn("aggregate").when(container).getAggregationIdentifier();
        Experiment experiment = mock(Experiment.class);
        doReturn(container).when(experiment).getContainer();
        doNothing().when(experiment).instantiateExperimentMethod();
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(false);
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(List.of(container)).when(firstPlatform).generateExperimentRoster();
        doReturn(true).when(container).canExperiment();
        doReturn(experiment).when(container).createExperiment();
        assertThat(experimentManager.scheduleExperiments(false), containsInAnyOrder(experiment));
    }

    @Test
    public void scheduleExperimentsForcedWithRoster () {
        Container container = mock(Container.class);
        doReturn("aggregate").when(container).getAggregationIdentifier();
        Experiment experiment = mock(Experiment.class);
        doReturn(container).when(experiment).getContainer();
        doNothing().when(experiment).instantiateExperimentMethod();
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(true);
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(List.of(container)).when(firstPlatform).generateExperimentRoster();
        doReturn(true).when(container).canExperiment();
        doReturn(experiment).when(container).createExperiment();
        assertThat(experimentManager.scheduleExperiments(true), containsInAnyOrder(experiment));
    }

    @Test
    public void scheduleExperimentsUnforcedWithoutRoster () {
        Container container = mock(Container.class);
        doReturn("aggregate").when(container).getAggregationIdentifier();
        Experiment experiment = mock(Experiment.class);
        doReturn(container).when(experiment).getContainer();
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(false);
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(Collections.emptyList()).when(firstPlatform).generateExperimentRoster();
        doReturn(true).when(container).canExperiment();
        doReturn(experiment).when(container).createExperiment();
        assertThat(experimentManager.scheduleExperiments(false), empty());
    }

    @Test
    public void scheduleExperimentsForcedWithoutRoster () {
        Container container = mock(Container.class);
        Experiment experiment = mock(Experiment.class);
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(true);
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(Collections.emptyList()).when(firstPlatform).generateExperimentRoster();
        doReturn(true).when(container).canExperiment();
        doReturn(experiment).when(container).createExperiment();
        assertThat(experimentManager.scheduleExperiments(true), empty());
    }

    @Test
    public void scheduleExperimentWithNoEligiblePlatforms () {
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.empty()).when(platformManager).getNextPlatformForExperiment(true);
        assertThat(experimentManager.scheduleExperiments(true), empty());
    }

    @Test
    public void scheduleExperimentWithNoPlatforms () {
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Collections.emptySet()).when(platformManager).getPlatforms();
        assertThat(experimentManager.scheduleExperiments(true), empty());
        verify(platformManager, never()).getNextPlatformForExperiment(anyBoolean());
    }

    @Test
    public void scheduleExperimentWithExistingExperiments () {
        experimentManager.addExperiment(mock(Experiment.class));
        assertThat(experimentManager.getAllExperiments(), iterableWithSize(1));
        assertThat(experimentManager.scheduleExperiments(true), empty());
        verify(platformManager, never()).getPlatforms();
    }

    @Test
    public void scheduleExperimentsThreadSafe () throws Exception {

        Container container = mock(Container.class);
        doReturn("aggregate").when(container).getAggregationIdentifier();
        Experiment experiment = mock(Experiment.class);
        doReturn(container).when(experiment).getContainer();
        doNothing().when(experiment).instantiateExperimentMethod();
        assertThat(experimentManager.getAllExperiments(), empty());
        doReturn(Optional.of(firstPlatform)).when(platformManager).getNextPlatformForExperiment(false);
        doReturn(firstPlatform).when(firstPlatform).scheduleExperiment();
        doReturn(List.of(container)).when(firstPlatform).generateExperimentRoster();
        doReturn(true).when(container).canExperiment();
        doReturn(experiment).when(container).createExperiment();
        Future<Set<Experiment>> futureSetOfExperiments;
        synchronized (experimentManager.getAllExperiments()) {
            futureSetOfExperiments = Executors.newSingleThreadExecutor()
                                              .submit(() -> experimentManager.scheduleExperiments(false));
            await().atMost(5, TimeUnit.SECONDS)
                   .until(() -> mockingDetails(container).getInvocations()
                                                         .stream()
                                                         .anyMatch(invocation -> invocation.getMethod()
                                                                                           .getName()
                                                                                           .equals("canExperiment")));
            assertFalse(futureSetOfExperiments.isDone());
        }
        await().atMost(5, TimeUnit.SECONDS)
               .until(() -> mockingDetails(container).getInvocations()
                                                     .stream()
                                                     .anyMatch(invocation -> invocation.getMethod()
                                                                                       .getName()
                                                                                       .equals("createExperiment")));
        await().atMost(1, TimeUnit.SECONDS).until(futureSetOfExperiments::isDone);
        assertThat(futureSetOfExperiments.get(), containsInAnyOrder(experiment));
    }

    @Test
    public void addExperiment () {
        Experiment experiment = mock(Experiment.class);
        assertSame(experiment, experimentManager.addExperiment(experiment));
        assertThat(experimentManager.getAllExperiments(), containsInAnyOrder(experiment));
    }

    @Test
    public void experimentContainerId () {
        Container firstContainer = mock(Container.class);
        Container secondContainer = mock(Container.class);
        long firstContainerIdentity = 12345678987654321L;
        long secondContainerIdentity = 98765432123456789L;
        doReturn(firstContainerIdentity).when(firstContainer).getIdentity();
        doReturn(secondContainerIdentity).when(secondContainer).getIdentity();
        doReturn(List.of(firstContainer)).when(firstPlatform).getRoster();
        doReturn(List.of(secondContainer)).when(secondPlatform).getRoster();
        Experiment firstContainerExperiment = mock(Experiment.class);
        doReturn(firstContainerExperiment).when(firstContainer).createExperiment();
        Experiment secondContainerExperiment = mock(Experiment.class);
        doReturn(secondContainerExperiment).when(secondContainer).createExperiment();
        doCallRealMethod().when(experimentManager).addExperiment(any());
        assertThat(experimentManager.experimentContainerId(firstContainerIdentity), containsInAnyOrder(firstContainerExperiment));
        assertThat(experimentManager.experimentContainerId(secondContainerIdentity), containsInAnyOrder(secondContainerExperiment));
    }

    @Test
    public void getExperimentByUUID () {
        String uuid = "this is just another random UUID";
        Experiment matchingExperiment = experimentManager.addExperiment(mock(Experiment.class));
        Experiment nonMatchingexperiment = experimentManager.addExperiment(mock(Experiment.class));
        doReturn(uuid).when(matchingExperiment).getId();
        doReturn(uuid + " that doesn't match").when(nonMatchingexperiment).getId();
        assertSame(matchingExperiment, experimentManager.getExperimentByUUID(uuid));
        assertNull(experimentManager.getExperimentByUUID(" with an extra string on the end to get a null"));
    }

    @Test
    public void getAllExperiments () {
        assertThat(experimentManager.getAllExperiments(), emptyIterable());
        Experiment experiment = experimentManager.addExperiment(mock(Experiment.class));
        assertThat(experimentManager.getAllExperiments(), containsInAnyOrder(experiment));
    }

    @Test
    public void evaluateExperiments () {
        Experiment experiment = experimentManager.addExperiment(mock(Experiment.class));
        doNothing().when(experimentManager).runExperimentSteps(experiment);
        experimentManager.evaluateExperiments();
        verify(experimentManager, times(1)).runExperimentSteps(experiment);
    }

    @Test
    public void runExperimentSteps () {
        Experiment experiment = experimentManager.addExperiment(mock(Experiment.class));
        final ExperimentState[] experimentStates = values();
        for (int i = 0; i < experimentStates.length; i++) {
            doReturn(experimentStates[i]).when(experiment).getExperimentState();
            experimentManager.runExperimentSteps(experiment);
            verify(experiment, times(i == 0 ? 1 : 0)).startExperiment();
            verify(experiment, times(i == 1 ? 1 : 0)).confirmStartupComplete();
            verify(experiment, times(i == 2 ? 1 : 0)).evaluateRunningExperiment();
            verify(experiment, times(i == 3 ? 1 : 0)).callSelfHealing();
            verify(experiment, times(i == 4 ? 1 : 0)).callFinalize();
            verify(experiment, times(i == 5 ? 1 : 0)).closeFinishedExperiment();
            verify(experiment, times(i == 6 ? 1 : 0)).closeFailedExperiment();
            reset(experiment);
        }
    }

    @Test
    public void scheduleExperimentSuite () {
        Experiment experiment1 = mock(Experiment.class);
        Experiment experiment2 = mock(Experiment.class);
        ExperimentSuite experimentSuite = new ExperimentSuite("firstPlatform", Map.of("aggregator", List.of("delete", "restart")));
        doReturn(Stream.of(experiment1, experiment2)).when(experimentManager)
                                                     .createSpecificExperiments(firstPlatform, "aggregator", List.of("delete", "restart"), 1);
        assertThat(experimentManager.scheduleExperimentSuite(experimentSuite), containsInAnyOrder(experiment1, experiment2));
    }

    @Test
    public void scheduleExperimentSuiteWithInvalidPlatform () {
        ExperimentSuite experimentSuite = new ExperimentSuite("fakePlatform", Collections.emptyMap());
        try {
            experimentManager.scheduleExperimentSuite(experimentSuite);
            fail("Exception expected");
        } catch (ChaosException e) {
            assertThat(e.getMessage(), Matchers.startsWith(String.valueOf(PLATFORM_DOES_NOT_EXIST.getErrorCode())));
        }
    }

    @Test
    public void scheduleExperimentSuiteWhileOtherExperimentsActive () {
        experimentManager.addExperiment(mock(Experiment.class));
        ExperimentSuite experimentSuite = new ExperimentSuite("firstPlatform", Map.of("aggregator", List.of("delete", "restart")));
        try {
            experimentManager.scheduleExperimentSuite(experimentSuite);
            fail("Exception expected");
        } catch (ChaosException e) {
            assertThat(e.getMessage(), Matchers.startsWith("13006"));
        }
        verify(firstPlatform, never()).getPlatformType();
        verify(secondPlatform, never()).getPlatformType();
    }

    @Test
    @Repeat(10)
    public void createSpecificExperiments () {
        Container container1 = mock(Container.class);
        Container container2 = mock(Container.class);
        Container container3 = mock(Container.class);
        Experiment experiment1 = mock(Experiment.class);
        Experiment experiment2 = mock(Experiment.class);
        Experiment experiment3 = mock(Experiment.class);
        doReturn(experiment1).when(container1).createExperiment(anyString());
        doReturn(experiment2).when(container2).createExperiment(anyString());
        doReturn(experiment3).when(container3).createExperiment(anyString());
        doReturn(Set.of(container1, container2, container3)).when(firstPlatform).getRosterByAggregationId("aggregate");
        Set<Experiment> experiments = experimentManager.createSpecificExperiments(firstPlatform, "aggregate", List.of("method1", "method2"), 1)
                                                       .collect(Collectors.toUnmodifiableSet());
        assertThat(experiments, Matchers.anyOf(containsInAnyOrder(experiment1, experiment2), containsInAnyOrder(experiment2, experiment3), containsInAnyOrder(experiment1, experiment3)));
    }

    @Test
    public void createSpecificExperimentsInsufficientContainerCountException () {
        doReturn(IntStream.range(0, 10)
                          .mapToObj(i -> mock(Container.class))
                          .collect(Collectors.toUnmodifiableSet())).when(firstPlatform)
                                                                   .getRosterByAggregationId("aggregate");
        try {
            experimentManager.createSpecificExperiments(firstPlatform, "aggregate", IntStream.range(0, 10)
                                                                                             .mapToObj(String::valueOf)
                                                                                             .collect(Collectors.toUnmodifiableList()), 1);
            fail("Should have thrown an exception");
        } catch (ChaosException e) {
            assertThat(e.getMessage(), Matchers.startsWith(String.valueOf(NOT_ENOUGH_CONTAINERS_FOR_PLANNED_EXPERIMENT.getErrorCode())));
        }
    }

    @Test
    public void createSpecificExperimentsInsufficientContainerCountExceptionMoreThanOneSurvivor () {
        doReturn(IntStream.range(0, 14)
                          .mapToObj(i -> mock(Container.class))
                          .collect(Collectors.toUnmodifiableSet())).when(firstPlatform)
                                                                   .getRosterByAggregationId("aggregate");
        try {
            experimentManager.createSpecificExperiments(firstPlatform, "aggregate", IntStream.range(0, 10)
                                                                                             .mapToObj(String::valueOf)
                                                                                             .collect(Collectors.toUnmodifiableList()), 5);
            fail("Should have thrown an exception");
        } catch (ChaosException e) {
            assertThat(e.getMessage(), Matchers.startsWith(String.valueOf(NOT_ENOUGH_CONTAINERS_FOR_PLANNED_EXPERIMENT.getErrorCode())));
        }
    }

    @Test(expected = Test.None.class /* No exception is expected */)
    public void createSpecificExperimentsWithNoSurvivors () {
        doReturn(IntStream.range(0, 10)
                          .mapToObj(i -> mock(Container.class))
                          .collect(Collectors.toUnmodifiableSet())).when(firstPlatform)
                                                                   .getRosterByAggregationId("aggregate");
        experimentManager.createSpecificExperiments(firstPlatform, "aggregate", IntStream.range(0, 10)
                                                                                         .mapToObj(String::valueOf)
                                                                                         .collect(Collectors.toUnmodifiableList()), 0);
        // Expect no exceptions.
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
            return spy(new ExperimentManager());
        }
    }
}