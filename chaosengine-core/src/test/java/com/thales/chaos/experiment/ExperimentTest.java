package com.thales.chaos.experiment;

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentState;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.scripts.ScriptManager;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.thales.chaos.container.enums.ContainerHealth.*;
import static com.thales.chaos.experiment.enums.ExperimentState.*;
import static com.thales.chaos.experiment.enums.ExperimentType.*;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ExperimentTest {
    private final Experiment experiment;
    private final Container container;
    private final ExperimentType experimentType;
    private AdminManager adminManager = mock(AdminManager.class);
    private HolidayManager holidayManager = mock(HolidayManager.class);
    private NotificationManager notificationManager = mock(NotificationManager.class);
    private ScriptManager scriptManager = mock(ScriptManager.class);

    public ExperimentTest (Experiment experiment, Container container, ExperimentType experimentType) {
        experiment.setHolidayManager(holidayManager);
        experiment.setAdminManager(adminManager);
        experiment.setNotificationManager(notificationManager);
        experiment.setScriptManager(scriptManager);
        this.experiment = spy(experiment);
        this.container = container;
        this.experimentType = experimentType;
    }

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> parameters () {
        Collection<Object[]> parameters = new ArrayList<>();
        Container stateContainer = spy(new MostlyAbstractContainer() {
            @ChaosExperiment(experimentType = ExperimentType.STATE)
            public void stateExperiment () {
            }
        });
        Experiment stateExperiment = new Experiment(stateContainer) {
        };
        parameters.add(new Object[]{ stateExperiment, stateContainer, STATE });
        Container networkContainer = spy(new MostlyAbstractContainer() {
            @ChaosExperiment(experimentType = NETWORK)
            public void networkExperiment () {
            }
        });
        Experiment networkExperiment = new Experiment(networkContainer) {
        };
        parameters.add(new Object[]{ networkExperiment, networkContainer, NETWORK });
        Container resourceContainer = spy(new MostlyAbstractContainer() {
            @ChaosExperiment(experimentType = RESOURCE)
            public void resourceExperiment () {
            }
        });
        Experiment resourceExperiment = new Experiment(resourceContainer) {
        };
        parameters.add(new Object[]{ resourceExperiment, resourceContainer, RESOURCE });
        return parameters;
    }

    @Before
    public void setUp () {
    }

    @Test
    public void getContainer () {
        assertSame(container, experiment.getContainer());
    }

    @Test
    public void getExperimentMethodName () {
        String expected = experimentType.toString().toLowerCase() + "Experiment";
        assertEquals(expected, experiment.getExperimentMethodName());
    }

    @Test
    public void startExperiment () {
        experiment.startExperiment();
        await().atMost(1, TimeUnit.SECONDS)
               .until(() -> mockingDetails(experiment).getInvocations()
                                                      .stream()
                                                      .map(InvocationOnMock::getMethod)
                                                      .map(Method::getName)
                                                      .anyMatch("startExperimentInner"::equals));
        verify(experiment).setExperimentState(STARTING);
    }

    @Test
    public void callSelfHealing () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        doNothing().when(experiment).evaluateRunningExperiment();
        doReturn(true).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> selfHealingMethodCalled.set(true));
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertTrue(selfHealingMethodCalled.get());
    }

    @Test
    public void callSelfHealingDisabled () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        doNothing().when(experiment).evaluateRunningExperiment();
        doReturn(false).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> selfHealingMethodCalled.set(true));
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertFalse(selfHealingMethodCalled.get());
    }

    @Test
    public void callSelfHealingException () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        doNothing().when(experiment).evaluateRunningExperiment();
        doReturn(true).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> {
            selfHealingMethodCalled.set(true);
            throw new RuntimeException();
        });
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertTrue(selfHealingMethodCalled.get());
    }

    @Test
    public void callSelfHealingLimitReached () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        experiment.setSelfHealingCounter(new AtomicInteger(9));
        doNothing().when(experiment).evaluateRunningExperiment();
        doReturn(true).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> selfHealingMethodCalled.set(true));
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertTrue(selfHealingMethodCalled.get());
        assertEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    public void callSelfHealingLimitNotYetReached () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        experiment.setSelfHealingCounter(new AtomicInteger(8));
        doNothing().when(experiment).evaluateRunningExperiment();
        doReturn(true).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> selfHealingMethodCalled.set(true));
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertTrue(selfHealingMethodCalled.get());
        assertEquals(SELF_HEALING, experiment.getExperimentState());
    }

    @Test
    public void callSelfHealingSuccessOnLastAttempt () {
        final AtomicBoolean selfHealingMethodCalled = new AtomicBoolean(false);
        experiment.setExperimentState(SELF_HEALING);
        experiment.setSelfHealingCounter(new AtomicInteger(9));
        doAnswer(invocationOnMock -> {
            experiment.setExperimentState(FINALIZING);
            return null;
        }).when(experiment).evaluateRunningExperiment();
        doReturn(true).when(experiment).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> selfHealingMethodCalled.set(true));
        experiment.callSelfHealing();
        verify(experiment).evaluateRunningExperiment();
        assertTrue(selfHealingMethodCalled.get());
        assertNotEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    public void canRunSelfHealingAdminDisabled () {
        doReturn(false).when(adminManager).canRunSelfHealing();
        assertFalse(experiment.canRunSelfHealing());
    }

    @Test
    public void canRunSelfHealingSuccess () {
        doReturn(true).when(adminManager).canRunSelfHealing();
        assertTrue(experiment.canRunSelfHealing());
    }

    @Test
    public void canRunSelfHealingFailDueToRecentExecution () {
        doReturn(true).when(adminManager).canRunSelfHealing();
        experiment.setSelfHealingMethod(() -> {
        });
        doReturn(true).when(experiment).canRunSelfHealing();
        doReturn(Duration.ofSeconds(30)).when(container).getMinimumSelfHealingInterval();
        doNothing().when(experiment).evaluateRunningExperiment();
        experiment.callSelfHealing();
        doCallRealMethod().when(experiment).canRunSelfHealing();
        assertFalse(experiment.canRunSelfHealing());
    }

    @Test
    public void evaluateRunningExperiment () {
        doReturn(NORMAL, DOES_NOT_EXIST, RUNNING_EXPERIMENT).when(experiment).checkContainerHealth();
        doReturn(true).when(experiment).isOverDuration();
        experiment.evaluateRunningExperiment();
        assertEquals(FINALIZING, experiment.getExperimentState());
        experiment.evaluateRunningExperiment();
        assertEquals(FAILED, experiment.getExperimentState());
        experiment.evaluateRunningExperiment();
        assertEquals(SELF_HEALING, experiment.getExperimentState());
    }

    @Test
    public void isOverDurationDueToAdminState () {
        doReturn(true).when(adminManager).mustRunSelfHealing();
        assertTrue("Admin Manager must run self healing, should be considered over duration", experiment.isOverDuration());
    }

    @Test
    public void isOverDurationBasedOnStartTime () {
        doReturn(false).when(adminManager).mustRunSelfHealing();
        doReturn(Instant.now().minus(Duration.ofHours(1))).when(experiment).getStartTime();
        assertTrue("Start time was an hour ago, should be over duration", experiment.isOverDuration());
    }

    @Test
    public void isOverDurationFalse () {
        doReturn(false).when(adminManager).mustRunSelfHealing();
        doReturn(Instant.now()).when(experiment).getStartTime();
        assertFalse("Start time is now, should not be over duration yet", experiment.isOverDuration());
    }

    @Test
    public void isBelowMinimumDurationFalse () {
        doReturn(Instant.now().minus(Duration.ofMinutes(1))).when(experiment).getStartTime();
        assertFalse(experiment.isBelowMinimumDuration());
    }

    @Test
    public void isBelowMinimumDurationTrue () {
        doReturn(Instant.now().minus(Duration.ofSeconds(1))).when(experiment).getStartTime();
        assertTrue(experiment.isBelowMinimumDuration());
    }

    @Test
    public void confirmStartupCompleteTrue () {
        doReturn(true).when(experiment).startExperimentInner(any());
        experiment.startExperiment();
        await().atMost(1, TimeUnit.SECONDS)
               .until(() -> mockingDetails(experiment).getInvocations()
                                                      .stream()
                                                      .map(InvocationOnMock::getMethod)
                                                      .map(Method::getName)
                                                      .anyMatch("startExperimentInner"::equals));
        experiment.confirmStartupComplete();
        assertEquals(STARTED, experiment.getExperimentState());
    }

    @Test
    public void confirmStartupCompleteFalse () {
        doReturn(false).when(experiment).startExperimentInner(any());
        experiment.startExperiment();
        await().atMost(1, TimeUnit.SECONDS)
               .until(() -> mockingDetails(experiment).getInvocations()
                                                      .stream()
                                                      .map(InvocationOnMock::getMethod)
                                                      .map(Method::getName)
                                                      .anyMatch("startExperimentInner"::equals));
        experiment.confirmStartupComplete();
        assertEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    public void confirmStartupCompleteRuntimeException () {
        doThrow(new RuntimeException()).when(experiment).startExperimentInner(any());
        experiment.startExperiment();
        await().atMost(1, TimeUnit.SECONDS)
               .until(() -> mockingDetails(experiment).getInvocations()
                                                      .stream()
                                                      .map(InvocationOnMock::getMethod)
                                                      .map(Method::getName)
                                                      .anyMatch("startExperimentInner"::equals));
        experiment.confirmStartupComplete();
        assertEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void confirmStartupCompleteInterrupted () throws ExecutionException, InterruptedException {
        Future mockFuture = mock(Future.class);
        doReturn(true).when(mockFuture).isDone();
        doThrow(new InterruptedException()).when(mockFuture).get();
        experiment.setExperimentStartup(mockFuture);
        experiment.confirmStartupComplete();
        assertEquals(FAILED, experiment.getExperimentState());
        assertTrue(Thread.interrupted());
    }

    @Test
    public void confirmStartupCompleteTimeout () {
        final AtomicBoolean interruptProcessed = new AtomicBoolean(false);
        final AtomicBoolean innerThreadStarted = new AtomicBoolean(false);
        Future<Boolean> futureBoolean = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                innerThreadStarted.set(true);
                Thread.sleep(1000000);
            } catch (InterruptedException e) {
                interruptProcessed.set(true);
            }
            return true;
        });
        experiment.setExperimentStartup(futureBoolean);
        experiment.setMaximumDuration(Duration.ZERO);
        await().atMost(1, TimeUnit.SECONDS).until(innerThreadStarted::get);
        experiment.confirmStartupComplete();
        await().atMost(1, TimeUnit.SECONDS).until(interruptProcessed::get);
        assertEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    public void callFinalizeNull () {
        experiment.setFinalizeMethod(null);
        experiment.callFinalize();
        assertEquals(FINISHED, experiment.getExperimentState());
    }

    @Test
    public void callFinalizeSuccess () {
        final AtomicBoolean finalizeMethodCalled = new AtomicBoolean(false);
        doReturn(Duration.ofSeconds(45)).when(experiment).getTimeInState();
        experiment.setExperimentState(FINALIZING);
        experiment.setFinalizeMethod(() -> finalizeMethodCalled.set(true));
        experiment.callFinalize();
        assertEquals(FINISHED, experiment.getExperimentState());
        assertTrue(finalizeMethodCalled.get());
    }

    @Test
    public void callFinalizeNotYetTime () {
        final AtomicBoolean finalizeMethodCalled = new AtomicBoolean(false);
        doReturn(Duration.ofSeconds(15)).when(experiment).getTimeInState();
        experiment.setExperimentState(FINALIZING);
        experiment.setFinalizeMethod(() -> finalizeMethodCalled.set(true));
        experiment.callFinalize();
        assertEquals(FINALIZING, experiment.getExperimentState());
        assertFalse(finalizeMethodCalled.get());
    }

    @Test
    public void callFinalizeException () {
        doReturn(Duration.ofSeconds(45)).when(experiment).getTimeInState();
        experiment.setExperimentState(FINALIZING);
        experiment.setFinalizeMethod(() -> {
            throw new RuntimeException();
        });
        experiment.callFinalize();
        assertEquals(FAILED, experiment.getExperimentState());
    }

    @Test
    public void getTimeInState () {
        experiment.setExperimentState(CREATED);
        experiment.setExperimentState(STARTING);
        assertTrue(experiment.getTimeInState().getSeconds() < 1);
        await().atLeast(500, TimeUnit.MILLISECONDS)
               .atMost(1, TimeUnit.SECONDS)
               .until(() -> experiment.getTimeInState().getNano() > 500000000);
    }

    @Test
    public void isComplete () {
        EnumMap<ExperimentState, Boolean> completeExperimentStateMap;
        completeExperimentStateMap = new EnumMap<>(ExperimentState.class);
        Arrays.stream(ExperimentState.values())
              .forEach(experimentState -> completeExperimentStateMap.put(experimentState, false));
        completeExperimentStateMap.put(FINISHED, true);
        completeExperimentStateMap.put(FAILED, true);
        completeExperimentStateMap.forEach((k, v) -> {
            experiment.setExperimentState(k);
            assertEquals(v, experiment.isComplete());
        });
    }

    @Test
    public void startExperimentInnerOutsideHours () {
        doReturn(true).when(experiment).cannotRunExperimentsNow();
        assertFalse(experiment.startExperimentInner(any()));
    }

    @Test
    public void startExperimentInnerContainerUnhealthy () {
        doReturn(false).when(experiment).cannotRunExperimentsNow();
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            if (!containerHealth.equals(NORMAL)) {
                doReturn(containerHealth).when(container).getContainerHealth(experimentType);
                assertFalse("Should not run experiment if container health is " + containerHealth, experiment.startExperimentInner(any()));
                verify(container, atLeastOnce()).getContainerHealth(experimentType);
            }
        }
    }

    @Test
    public void startExperimentInnerSuccess () {
        doReturn(false).when(experiment).cannotRunExperimentsNow();
        doReturn(NORMAL).when(container).getContainerHealth(experimentType);
        doNothing().when(container).startExperiment(experiment);
        Instant timeBeforeStart = Instant.now();
        assertTrue(experiment.startExperimentInner(any()));
        verify(container).startExperiment(experiment);
        assertTrue(timeBeforeStart.isBefore(experiment.getStartTime()));
    }

    @Test
    public void startExperimentInnerFail () {
        doReturn(false).when(experiment).cannotRunExperimentsNow();
        doReturn(NORMAL).when(container).getContainerHealth(experimentType);
        doThrow(new ChaosException(ChaosErrorCode.EXPERIMENT_START_FAILURE)).when(container)
                                                                            .startExperiment(experiment);
        try {
            experiment.startExperimentInner(any());
            fail("Exception expected");
        } catch (ChaosException e) {
            assertThat(e.getMessage(), Matchers.startsWith("12001"));
        }
        verify(container).startExperiment(experiment);
    }

    @Test
    public void cannotRunExperimentsNowDueToHoliday () {
        doReturn(true).when(holidayManager).isHoliday();
        assertTrue(experiment.cannotRunExperimentsNow());
        verify(holidayManager).isHoliday();
    }

    @Test
    public void cannotRunExperimentsNowDueToWorkingHours () {
        doReturn(false).when(holidayManager).isHoliday();
        doReturn(true).when(holidayManager).isOutsideWorkingHours();
        assertTrue(experiment.cannotRunExperimentsNow());
        verify(holidayManager).isOutsideWorkingHours();
    }

    @Test
    public void cannotRunExperimentsNowDueToAdminState () {
        doReturn(false).when(holidayManager).isHoliday();
        doReturn(false).when(holidayManager).isOutsideWorkingHours();
        doReturn(false).when(adminManager).canRunExperiments();
        assertTrue(experiment.cannotRunExperimentsNow());
        verify(adminManager).canRunExperiments();
    }

    @Test
    public void cannotRunExperimentsFalse () {
        doReturn(false).when(holidayManager).isHoliday();
        doReturn(false).when(holidayManager).isOutsideWorkingHours();
        doReturn(true).when(adminManager).canRunExperiments();
        assertFalse(experiment.cannotRunExperimentsNow());
        verify(holidayManager).isHoliday();
        verify(holidayManager).isOutsideWorkingHours();
        verify(adminManager).canRunExperiments();
    }

    @Test
    public void checkContainerHealthTooEarly () {
        doReturn(true).when(experiment).isBelowMinimumDuration();
        assertEquals(RUNNING_EXPERIMENT, experiment.checkContainerHealth());
    }

    @Test
    public void checkContainerHealthWithMethod () {
        final AtomicBoolean checkMethodCalled = new AtomicBoolean(false);
        experiment.setCheckContainerHealth(() -> {
            checkMethodCalled.set(true);
            return NORMAL;
        });
        doReturn(false).when(experiment).isBelowMinimumDuration();
        assertEquals(NORMAL, experiment.checkContainerHealth());
        assertTrue(checkMethodCalled.get());
    }

    @Test
    public void checkContainerHealthWithMethodCausingException () {
        final AtomicBoolean checkMethodCalled = new AtomicBoolean(false);
        experiment.setCheckContainerHealth(() -> {
            checkMethodCalled.set(true);
            throw new RuntimeException();
        });
        doReturn(false).when(experiment).isBelowMinimumDuration();
        assertEquals(RUNNING_EXPERIMENT, experiment.checkContainerHealth());
        assertTrue(checkMethodCalled.get());
    }

    @Test
    public void checkContainerHealthWithNoMethod () {
        experiment.setCheckContainerHealth(null);
        doReturn(false).when(experiment).isBelowMinimumDuration();
        for (ContainerHealth containerHealth : ContainerHealth.values()) {
            doReturn(containerHealth).when(container).getContainerHealth(experimentType);
            assertEquals(containerHealth, experiment.checkContainerHealth());
        }
    }

    @Test
    public void checkContainerHealthWithNoMethodCausingException () {
        experiment.setCheckContainerHealth(null);
        doReturn(false).when(experiment).isBelowMinimumDuration();
        doThrow(new RuntimeException()).when(container).getContainerHealth(experimentType);
        assertEquals(RUNNING_EXPERIMENT, experiment.checkContainerHealth());
    }

    private static abstract class MostlyAbstractContainer extends Container {
        @Override
        public Platform getPlatform () {
            return null;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return null;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public String getAggregationIdentifier () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }
    }
}
