package com.thales.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.constants.ExperimentConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.enums.ExperimentState;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.scripts.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thales.chaos.constants.ExperimentConstants.*;
import static java.util.UUID.randomUUID;
import static net.logstash.logback.argument.StructuredArguments.v;

public abstract class Experiment {
    private static final Logger log = LoggerFactory.getLogger(Experiment.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final String id = randomUUID().toString();
    private final AtomicReference<ExperimentState> experimentState = new AtomicReference<>(ExperimentState.CREATED);
    protected Container container;
    protected ExperimentType experimentType;
    protected Duration minimumDuration = Duration.ofSeconds(ExperimentConstants.DEFAULT_EXPERIMENT_MINIMUM_DURATION_SECONDS);
    protected Duration maximumDuration = Duration.ofMinutes(ExperimentConstants.DEFAULT_EXPERIMENT_DURATION_MINUTES);
    protected Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
    private Platform experimentLayer;
    private ExperimentMethod experimentMethod;
    @Autowired
    private NotificationManager notificationManager;
    @Autowired
    private AdminManager adminManager;
    @Autowired
    private ScriptManager scriptManager;
    @Autowired
    private HolidayManager holidayManager;
    private Callable<Void> selfHealingMethod = () -> null;
    private Callable<ContainerHealth> checkContainerHealth;
    private Callable<Void> finalizeMethod;
    private Instant startTime = Instant.now();
    private Instant transitionTime = Instant.now();
    private Instant endTime;
    private Instant lastSelfHealingTime;
    private Duration totalExperimentDuration;
    private AtomicInteger selfHealingCounter = new AtomicInteger(0);
    @Value("${preferredExperiment:#{null}}")
    private String preferredExperiment;
    private Future<Boolean> experimentStartup;

    public Experiment (Container container, ExperimentType experimentType) {
        this.container = container;
        this.experimentType = experimentType;
        this.experimentMethod = chooseExperimentMethodConsumer();
        setExperimentLayer(container.getPlatform());
    }

    private <T extends Container> ExperimentMethod<T> chooseExperimentMethodConsumer () {
        Collection<ExperimentMethod<T>> reflectionBasedMethods = getReflectionBasedMethods();
        Collection<ExperimentMethod<T>> scriptBasedMethods = getScriptBasedMethods();
        Collection<ExperimentMethod<T>> allMethods = Stream.concat(scriptBasedMethods.stream(), reflectionBasedMethods.stream())
                                                           .filter(m -> !m.isCattleOnly() || getContainer().isCattle())
                                                           .collect(Collectors.toSet());
        Optional<ExperimentMethod<T>> preferredMethod = allMethods.stream()
                                                                  .filter(method -> method.getExperimentName()
                                                                                          .equals(preferredExperiment))
                                                                  .findFirst();
        ExperimentMethod<T> method;
        if (preferredMethod.isPresent()) {
            method = preferredMethod.get();
            ExperimentType newExperimentType = method.getExperimentType();
            log.info("Preferred experiment chosen, changing experiment type to {}", v("experimentType", newExperimentType));
            this.experimentType = newExperimentType;
        } else {
            method = allMethods.stream().filter(m -> experimentType.equals(m.getExperimentType()))
                               .sorted(Comparator.comparingInt(i -> new Random().nextInt()))
                               .findAny()
                               .orElse(null);
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    private <T extends Container> Collection<ExperimentMethod<T>> getReflectionBasedMethods () {
        return getContainer().getExperimentMethods()
                             .values()
                             .stream()
                             .flatMap(Collection::stream)
                             .map(ExperimentMethod::fromMethod)
                             .map(method -> (ExperimentMethod<T>) method)
                             .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private <T extends Container> Collection<ExperimentMethod<T>> getScriptBasedMethods () {
        if (!getContainer().supportsShellBasedExperiments()) return Collections.emptySet();
        final Collection<String> knownMissingCapabilities = container.getKnownMissingCapabilities();
        return scriptManager.getScripts()
                            .stream()
                            .filter(script -> script.getDependencies()
                                                    .stream()
                                                    .noneMatch(knownMissingCapabilities::contains))
                            .map(script -> ExperimentMethod.fromScript(getContainer(), script))
                            .map(method -> (ExperimentMethod<T>) method)
                            .collect(Collectors.toSet());
    }

    public Container getContainer () {
        return container;
    }

    public String getExperimentMethodName () {
        return Optional.ofNullable(experimentMethod)
                       .map(ExperimentMethod::getExperimentName)
                       .orElse(EXPERIMENT_METHOD_NOT_SET_YET);
    }

    void startExperiment () {
        setExperimentStartup(executorService.submit(this::startExperimentInner));
        setExperimentState(ExperimentState.STARTING);
    }

    void setExperimentStartup (Future<Boolean> experimentStartup) {
        this.experimentStartup = experimentStartup;
    }

    boolean startExperimentInner () {
        Map<String, String> existingMDC = MDC.getCopyOfContextMap();
        if (existingMDC == null) existingMDC = Collections.emptyMap();
        try {
            MDC.setContextMap(existingMDC);
            if (cannotRunExperimentsNow()) {
                return Boolean.FALSE;
            }
            if (container.getContainerHealth(experimentType) != ContainerHealth.NORMAL) {
                log.info("Failed to start an experiment as this container is already in an abnormal state\n{}", container);
                return Boolean.FALSE;
            }
            if (experimentMethod == null) {
                log.error("No method is available for experimentation");
                return Boolean.FALSE;
            }
            notificationManager.sendNotification(ChaosExperimentEvent.builder()
                                                                     .fromExperiment(this)
                                                                     .withNotificationLevel(NotificationLevel.WARN)
                                                                     .withMessage(STARTING_NEW_EXPERIMENT)
                                                                     .build());
            try {
                container.startExperiment(this);
                startTime = Instant.now();
                return Boolean.TRUE;
            } catch (ChaosException ex) {
                notificationManager.sendNotification(ChaosExperimentEvent.builder()
                                                                         .fromExperiment(this)
                                                                         .withNotificationLevel(NotificationLevel.ERROR)
                                                                         .withMessage(FAILED_TO_START_EXPERIMENT)
                                                                         .build());
                return Boolean.FALSE;
            }
        } finally {
            existingMDC.keySet().forEach(MDC::remove);
        }
    }

    boolean cannotRunExperimentsNow () {
        if (holidayManager.isHoliday()) {
            log.info("Cannot start an experiment right now. Enjoy the holiday");
            return true;
        } else if (holidayManager.isOutsideWorkingHours()) {
            log.info("Cannot start an experiment right now. Come back during working hours");
            return true;
        } else if (!adminManager.canRunExperiments()) {
            log.info("Cannot start an experiment right now. Current admin state is {}", adminManager.getAdminState());
            return true;
        }
        return false;
    }

    public void setScriptManager (ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public void setPreferredExperiment (String preferredExperiment) {
        this.preferredExperiment = preferredExperiment;
    }

    NotificationManager getNotificationManager () {
        return notificationManager;
    }

    public void setNotificationManager (NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public Instant getLastSelfHealingTime () {
        return lastSelfHealingTime;
    }

    @JsonIgnore
    public Platform getExperimentLayer () {
        return experimentLayer;
    }

    private void setExperimentLayer (Platform experimentLayer) {
        this.experimentLayer = experimentLayer;
    }

    public String getExperimentLayerName () {
        return container.getContainerType();
    }

    @JsonIgnore
    public ExperimentMethod getExperimentMethod () {
        return experimentMethod;
    }

    private void setExperimentMethod (ExperimentMethod experimentMethod) {
        this.experimentMethod = experimentMethod;
    }

    public Callable<Void> getSelfHealingMethod () {
        return selfHealingMethod;
    }

    public void setSelfHealingMethod (Callable<Void> selfHealingMethod) {
        this.selfHealingMethod = selfHealingMethod;
    }

    @JsonIgnore
    public Callable<Void> getFinalizeMethod () {
        return finalizeMethod;
    }

    public void setFinalizeMethod (Callable<Void> finalizeMethod) {
        this.finalizeMethod = finalizeMethod;
    }

    @JsonIgnore
    public Callable<ContainerHealth> getCheckContainerHealth () {
        return checkContainerHealth;
    }

    public void setCheckContainerHealth (Callable<ContainerHealth> checkContainerHealth) {
        this.checkContainerHealth = checkContainerHealth;
    }

    public void setFinalizationDuration (Duration finalizationDuration) {
        this.finalizationDuration = finalizationDuration;
    }

    public void setHolidayManager (HolidayManager holidayManager) {
        this.holidayManager = holidayManager;
    }

    public String getId () {
        return id;
    }

    public ExperimentType getExperimentType () {
        return experimentType;
    }

    private void setEndTime () {
        setEndTime(Instant.now());
    }

    protected Duration getDuration () {
        return Duration.between(getStartTime(), getEndTime());
    }

    public Instant getStartTime () {
        return startTime;
    }

    private Instant getEndTime () {
        return Optional.ofNullable(endTime).orElseGet(Instant::now);
    }

    void setEndTime (Instant endTime) {
        this.endTime = endTime;
    }

    void callSelfHealing () {
        int selfHealingAttempts = getSelfHealingCounter().get();
        try {
            if (canRunSelfHealing()) {
                selfHealingAttempts = getSelfHealingCounter().incrementAndGet();
                lastSelfHealingTime = Instant.now();
                selfHealingMethod.call();
            }
        } catch (Exception e) {
            log.error("An error occurred while calling self healing method", e);
        } finally {
            evaluateRunningExperiment();
            if (selfHealingAttempts >= DEFAULT_MAXIMUM_SELF_HEALING_RETRIES && getExperimentState().equals(ExperimentState.SELF_HEALING)) {
                setExperimentState(ExperimentState.FAILED);
            }
        }
    }

    public AtomicInteger getSelfHealingCounter () {
        return selfHealingCounter;
    }

    protected void setSelfHealingCounter (AtomicInteger selfHealingCounter) {
        this.selfHealingCounter = selfHealingCounter;
    }

    boolean canRunSelfHealing () {
        if (!adminManager.canRunSelfHealing()) {
            log.debug("Self healing disallowed by Admin State {}", v("AdminState", adminManager.getAdminState()));
            return false;
        } else if (Optional.ofNullable(lastSelfHealingTime)
                           .map(i -> i.plus(getMinimumTimeBetweenSelfHealing()))
                           .map(i -> i.isAfter(Instant.now()))
                           .orElse(false)) {
            log.debug("Self healing last occurred too recently. Skipping re-execution.");
            return false;
        }
        return true;
    }

    void evaluateRunningExperiment () {
        switch (checkContainerHealth()) {
            case NORMAL:
                setExperimentState(ExperimentState.FINALIZING);
                break;
            case RUNNING_EXPERIMENT:
                if (isOverDuration()) setExperimentState(ExperimentState.SELF_HEALING);
                break;
            case DOES_NOT_EXIST:
                setExperimentState(ExperimentState.FAILED);
                break;
        }
    }

    public ExperimentState getExperimentState () {
        return experimentState.get();
    }

    void setExperimentState (ExperimentState experimentState) {
        ExperimentState oldExperimentState = this.experimentState.get();
        if (!experimentState.equals(oldExperimentState)) {
            log.debug("Experiment transitioning from {} to {}", v("oldExperimentState", oldExperimentState), v("experimentState", experimentState));
            this.transitionTime = Instant.now();
            this.experimentState.set(experimentState);
            if (experimentState.isComplete()) {
                totalExperimentDuration = Duration.between(startTime, Instant.now());
            }
        }
    }

    private Duration getMinimumTimeBetweenSelfHealing () {
        return container.getMinimumSelfHealingInterval();
    }

    ContainerHealth checkContainerHealth () {
        if (isBelowMinimumDuration()) {
            log.debug("Experiment is too young to evaluate health.");
            return ContainerHealth.RUNNING_EXPERIMENT;
        }
        if (checkContainerHealth != null) {
            try {
                return checkContainerHealth.call();
            } catch (Exception e) {
                log.error("Issue while checking container health using specific method", e);
                return ContainerHealth.RUNNING_EXPERIMENT;
            }
        }
        try {
            return container.getContainerHealth(experimentType);
        } catch (Exception e) {
            log.error("Issue while checking container health", e);
            return ContainerHealth.RUNNING_EXPERIMENT;
        }
    }

    boolean isOverDuration () {
        return adminManager.mustRunSelfHealing() || Instant.now().isAfter(getStartTime().plus(maximumDuration));
    }

    boolean isBelowMinimumDuration () {
        return Instant.now().isBefore(getStartTime().plus(minimumDuration));
    }

    void confirmStartupComplete () {
        if (experimentStartup.isDone()) {
            try {
                setExperimentState(experimentStartup.get() ? ExperimentState.STARTED : ExperimentState.FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Exception occurred while starting experiment", e);
                setExperimentState(ExperimentState.FAILED);
            } catch (ExecutionException e) {
                log.error("Exception occurred while starting experiment", e);
                setExperimentState(ExperimentState.FAILED);
            }
        }
    }

    void callFinalize () {
        if (finalizeMethod == null) {
            setExperimentState(ExperimentState.FINISHED);
            return;
        }
        try {
            if (getTimeInState().compareTo(finalizationDuration) >= 0) {
                finalizeMethod.call();
                setExperimentState(ExperimentState.FINISHED);
            }
        } catch (Exception e) {
            log.error("Error running finalization command", e);
            setExperimentState(ExperimentState.FAILED);
        }
    }

    Duration getTimeInState () {
        return Duration.between(transitionTime, Instant.now());
    }

    boolean isComplete () {
        return getExperimentState().isComplete();
    }

    void closeFinishedExperiment () {
        log.info("Experiment ended with duration of {}", v("finalExperimentDuration", totalExperimentDuration));
    }

    void closeFailedExperiment () {
        log.error("Experiment failed with duration of {}", v("finalExperimentDuration", totalExperimentDuration));
    }

    public void setAdminManager (AdminManager adminManager) {
        this.adminManager = adminManager;
    }
}
