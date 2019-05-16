package com.thales.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.constants.ExperimentConstants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.enums.ExperimentState;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.ChaosEvent;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.enums.NotificationLevel;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thales.chaos.constants.ExperimentConstants.*;
import static com.thales.chaos.exception.enums.ChaosErrorCode.SELF_HEALING_CALL_ERROR;
import static java.util.UUID.randomUUID;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

public abstract class Experiment {
    private static final Logger log = LoggerFactory.getLogger(Experiment.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final String id = randomUUID().toString();
    protected Container container;
    protected ExperimentType experimentType;
    protected Duration minimumDuration = Duration.ofSeconds(ExperimentConstants.DEFAULT_EXPERIMENT_MINIMUM_DURATION_SECONDS);
    protected Duration maximumDuration = Duration.ofMinutes(ExperimentConstants.DEFAULT_EXPERIMENT_DURATION_MINUTES);
    protected Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
    private Platform experimentLayer;
    private ExperimentMethod experimentMethod;
    private ExperimentState experimentState = ExperimentState.NOT_YET_STARTED;
    @Autowired
    private NotificationManager notificationManager;
    @Autowired
    private AdminManager adminManager;
    @Autowired
    private ScriptManager scriptManager;
    private Callable<Void> selfHealingMethod = () -> null;
    private Callable<ContainerHealth> checkContainerHealth;
    private Callable<Void> finalizeMethod;
    private Instant startTime = Instant.now();
    private Instant endTime;
    private Instant finalizationStartTime;
    private Instant lastSelfHealingTime;
    private AtomicInteger selfHealingCounter = new AtomicInteger(0);
    @Value("${preferredExperiment:#{null}}")
    private String preferredExperiment;

    public void setScriptManager (ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    Boolean isSelfHealingRequired () {
        return endTime != null ? getSelfHealingCounter().get() > 0 : null;
    }

    public AtomicInteger getSelfHealingCounter () {
        return selfHealingCounter;
    }

    protected void setSelfHealingCounter (AtomicInteger selfHealingCounter) {
        this.selfHealingCounter = selfHealingCounter;
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

    public String getExperimentMethodName () {
        if (experimentMethod != null) {
            return experimentMethod.getExperimentName();
        }
        return EXPERIMENT_METHOD_NOT_SET_YET;
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

    Future<Boolean> startExperiment () {
        MDC.put(DataDogConstants.DATADOG_EXPERIMENTID_KEY, getId());
        getContainer().setMappedDiagnosticContext();
        Map<String, String> existingMDC = MDC.getCopyOfContextMap();
        MDC.remove(DataDogConstants.DATADOG_EXPERIMENTID_KEY);
        getContainer().clearMappedDiagnosticContext();
        return executorService.submit(() -> {
            try {
                MDC.setContextMap(existingMDC);
                if (!adminManager.canRunExperiments()) {
                    log.info("Cannot start experiments right now, system is {}", adminManager.getAdminState());
                    return Boolean.FALSE;
                }
                if (container.getContainerHealth(experimentType) != ContainerHealth.NORMAL) {
                    log.info("Failed to start an experiment as this container is already in an abnormal state\n{}", container);
                    return Boolean.FALSE;
                }
                if (container.supportsExperimentType(experimentType)) {
                    ExperimentMethod chosenMethod = chooseExperimentMethodConsumer();
                    if (chosenMethod == null) return Boolean.FALSE;
                    log.info("Chosen {} for experiment {}", kv("experimentMethod", chosenMethod.getExperimentName()), v(DataDogConstants.DATADOG_EXPERIMENTID_KEY, id));
                    setExperimentMethod(chosenMethod);
                    setExperimentLayer(container.getPlatform());
                    notificationManager.sendNotification(ChaosEvent.builder()
                                                                   .fromExperiment(this)
                                                                   .withNotificationLevel(NotificationLevel.WARN)
                                                                   .withMessage(ExperimentConstants.STARTING_NEW_EXPERIMENT)
                                                                   .build());
                    try {
                        container.startExperiment(this);
                    } catch (ChaosException ex) {
                        notificationManager.sendNotification(ChaosEvent.builder()
                                                                       .fromExperiment(this)
                                                                       .withNotificationLevel(NotificationLevel.ERROR)
                                                                       .withMessage(ExperimentConstants.FAILED_TO_START_EXPERIMENT)
                                                                       .build());
                        return Boolean.FALSE;
                    }
                    startTime = Instant.now();
                    experimentState = ExperimentState.STARTED;
                } else {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            } finally {
                existingMDC.keySet().forEach(MDC::remove);
            }
        });
    }

    public String getId () {
        return id;
    }

    public Container getContainer () {
        return container;
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
            method = allMethods.stream()
                               .filter(m -> m.getExperimentType().equals(experimentType))
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

    public ExperimentType getExperimentType () {
        return experimentType;
    }

    @JsonIgnore
    public ExperimentState getExperimentState () {
        experimentState = checkExperimentState();
        if (experimentState == ExperimentState.FAILED || experimentState == ExperimentState.FINISHED) {
            setEndTime();
        }
        return experimentState;
    }

    private synchronized ExperimentState checkExperimentState () {
        switch (checkContainerHealth()) {
            case NORMAL:
                if (isFinalizable()) {
                    log.info("Experiment {} complete", id);
                    notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                                   .withNotificationLevel(NotificationLevel.GOOD)
                                                                   .withMessage(String.format("Experiment finished. Container recovered from the experiment. Duration: %d s, Self-healing attempts: %d", getDuration()
                                                                           .getSeconds(), selfHealingCounter.get()))
                                                                   .build());
                    return finalizeExperiment();
                }
                return ExperimentState.STARTED;
            case DOES_NOT_EXIST:
                log.info("Experiment {} no longer maps to existing container", id);
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Container no longer exists.")
                                                               .build());
                return ExperimentState.FAILED;
            case RUNNING_EXPERIMENT:
            default:
                return doSelfHealing();
        }
    }

    private void setEndTime () {
        setEndTime(Instant.now());
    }

    private ContainerHealth checkContainerHealth () {
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

    public boolean isFinalizable () {
        if (getFinalizationStartTime() == null) {
            finalizationStartTime = Instant.now();
        }
        boolean finalizable = Instant.now().isAfter(getFinalizationStartTime().plus(finalizationDuration));
        log.debug("Experiment {} is finalizable = {}", id, finalizable);
        return finalizable;
    }

    protected Duration getDuration () {
        return Duration.between(getStartTime(), getEndTime());
    }

    private ExperimentState finalizeExperiment () {
        if (finalizeMethod != null) {
            try {
                log.info("Finalizing experiment {} on container {}", id, container.getSimpleName());
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.WARN)
                                                               .withMessage("Running experiment finalization method")
                                                               .build());
                finalizeMethod.call();
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.GOOD)
                                                               .withMessage("Experiment finalization done")
                                                               .build());
            } catch (Exception e) {
                log.error("Error while finalizing experiment {} on container {}", id, container.getSimpleName());
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Error while finalizing experiment")
                                                               .build());
                return ExperimentState.FAILED;
            }
        }
        return ExperimentState.FINISHED;
    }

    public ExperimentState doSelfHealing () {
        if (isOverDuration()) {
            try {
                log.warn("The experiment {} has gone on too long, invoking self-healing. \n{}", id, this);
                ChaosEvent chaosEvent;
                if (canRunSelfHealing()) {
                    if (selfHealingCounter.get() <= DEFAULT_MAXIMUM_SELF_HEALING_RETRIES) {
                        StringBuilder message = new StringBuilder();
                        message.append(ExperimentConstants.THE_EXPERIMENT_HAS_GONE_ON_TOO_LONG_INVOKING_SELF_HEALING);
                        NotificationLevel notificationLevel = NotificationLevel.ERROR;
                        if (selfHealingCounter.incrementAndGet() > 1) {
                            message.append(ExperimentConstants.THIS_IS_SELF_HEALING_ATTEMPT_NUMBER)
                                   .append(selfHealingCounter.get())
                                   .append(".");
                            notificationLevel = NotificationLevel.WARN;
                        }
                        chaosEvent = ChaosEvent.builder()
                                               .fromExperiment(this)
                                               .withNotificationLevel(notificationLevel)
                                               .withMessage(message.toString())
                                               .build();
                        notificationManager.sendNotification(chaosEvent);
                        callSelfHealing();
                    } else {
                        chaosEvent = ChaosEvent.builder()
                                               .fromExperiment(this)
                                               .withNotificationLevel(NotificationLevel.ERROR)
                                               .withMessage(MAXIMUM_SELF_HEALING_RETRIES_REACHED)
                                               .build();
                        notificationManager.sendNotification(chaosEvent);
                        return ExperimentState.FAILED;
                    }
                } else if (adminManager.canRunSelfHealing()) {
                    chaosEvent = ChaosEvent.builder().fromExperiment(this)
                                           .withNotificationLevel(NotificationLevel.WARN)
                                           .withMessage(ExperimentConstants.CANNOT_RUN_SELF_HEALING_AGAIN_YET)
                                           .build();
                    notificationManager.sendNotification(chaosEvent);
                } else {
                    chaosEvent = ChaosEvent.builder().fromExperiment(this)
                                           .withNotificationLevel(NotificationLevel.WARN)
                                           .withMessage(ExperimentConstants.SYSTEM_IS_PAUSED_AND_UNABLE_TO_RUN_SELF_HEALING)
                                           .build();
                    notificationManager.sendNotification(chaosEvent);
                }
            } catch (ChaosException e) {
                log.error("Experiment {}: An exception occurred while running self-healing.", id, e);
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage(ExperimentConstants.AN_EXCEPTION_OCCURRED_WHILE_RUNNING_SELF_HEALING)
                                                               .build());
            }
        } else {
            notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                           .withNotificationLevel(NotificationLevel.WARN)
                                                           .withMessage("Experiment not yet finished.")
                                                           .build());
        }
        return ExperimentState.STARTED;
    }

    public boolean isBelowMinimumDuration () {
        return Instant.now().isBefore(startTime.plus(minimumDuration));
    }

    public Instant getFinalizationStartTime () {
        return finalizationStartTime;
    }

    public Instant getStartTime () {
        return startTime;
    }

    private Instant getEndTime () {
        return Optional.ofNullable(endTime).orElseGet(Instant::now);
    }

    protected boolean isOverDuration () {
        return adminManager.mustRunSelfHealing() || Instant.now().isAfter(getStartTime().plus(maximumDuration));
    }

    protected boolean canRunSelfHealing () {
        boolean canRunSelfHealing = lastSelfHealingTime == null || lastSelfHealingTime.plus(getMinimumTimeBetweenSelfHealing())
                                                                                      .isBefore(Instant.now());
        return canRunSelfHealing && adminManager.canRunSelfHealing();
    }

    public void callSelfHealing () {
        try {
            selfHealingMethod.call();
        } catch (Exception e) {
            throw new ChaosException(SELF_HEALING_CALL_ERROR, e);
        } finally {
            lastSelfHealingTime = Instant.now();
        }
    }

    private Duration getMinimumTimeBetweenSelfHealing () {
        return container.getMinimumSelfHealingInterval();
    }

    void setEndTime (Instant endTime) {
        this.endTime = endTime;
    }
}
