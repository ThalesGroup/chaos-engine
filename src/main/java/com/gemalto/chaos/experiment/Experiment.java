package com.gemalto.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.constants.ExperimentConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.enums.ExperimentState;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.ExperimentConstants.*;
import static com.gemalto.chaos.util.MethodUtils.getMethodsWithAnnotation;
import static java.util.UUID.randomUUID;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

public abstract class Experiment {
    private static final Logger log = LoggerFactory.getLogger(Experiment.class);
    private final String id = randomUUID().toString();
    protected Container container;
    protected ExperimentType experimentType;
    protected Duration minimumDuration = Duration.ofSeconds(ExperimentConstants.DEFAULT_EXPERIMENT_MINIMUM_DURATION_SECONDS);
    protected Duration maximumDuration = Duration.ofMinutes(ExperimentConstants.DEFAULT_EXPERIMENT_DURATION_MINUTES);
    protected Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
    private Platform experimentLayer;
    private Method experimentMethod;
    private ExperimentState experimentState = ExperimentState.NOT_YET_STARTED;
    @Autowired
    private NotificationManager notificationManager;
    @Autowired
    private AdminManager adminManager;
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

    public AtomicInteger getSelfHealingCounter () {
        return selfHealingCounter;
    }

    protected void setSelfHealingCounter (AtomicInteger selfHealingCounter) {
        this.selfHealingCounter = selfHealingCounter;
    }

    @JsonIgnore
    public Method getExperimentMethod () {
        return experimentMethod;
    }

    private void setExperimentMethod (Method experimentMethod) {
        this.experimentMethod = experimentMethod;
    }

    public String getExperimentMethodName(){
        if(experimentMethod!=null) {
            return experimentMethod.getName();
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

    public String getId () {
        return id;
    }

    public Container getContainer () {
        return container;
    }

    Future<Boolean> startExperiment () {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        MDC.put(DataDogConstants.DATADOG_EXPERIMENTID_KEY, getId());
        getContainer().setMappedDiagnosticContext();
        Map<String, String> existingMDC = MDC.getCopyOfContextMap();
        Executors.newCachedThreadPool().submit(() -> {
            try {
                MDC.setContextMap(existingMDC);
                if (!adminManager.canRunExperiments()) {
                    log.info("Cannot start experiments right now, system is {}", adminManager.getAdminState());
                    completableFuture.complete(false);
                    return null;
                }
                if (container.getContainerHealth(experimentType) != ContainerHealth.NORMAL) {
                    log.info("Failed to start an experiment as this container is already in an abnormal state\n{}", container);
                    completableFuture.complete(false);
                    return null;
                }
                if (container.supportsExperimentType(experimentType)) {
                    List<Method> experimentMethods = getMethodsWithAnnotation(container.getClass(), getExperimentType().getAnnotation());
                    if (experimentMethods.isEmpty()) {
                        completableFuture.cancel(false);
                        throw new ChaosException("Could not find an experiment vector");
                    }
                    Method chosenMethod = null;
                    if (preferredExperiment != null && experimentMethods.stream()
                                                                        .map(Method::getName)
                                                                        .anyMatch(s -> s.equals(preferredExperiment))) {
                        Map<String, Method> stringMethodMap = experimentMethods.stream()
                                                                               .collect(Collectors.toMap(Method::getName, method -> method));
                        chosenMethod = stringMethodMap.get(preferredExperiment);
                        log.debug("Preferred method {} was mapped to {} method", preferredExperiment, chosenMethod);
                    }
                    if (chosenMethod == null) {
                        int index = ThreadLocalRandom.current().nextInt(experimentMethods.size());
                        chosenMethod = experimentMethods.get(index);
                    }
                    log.info("Chosen {} for experiment {}", kv("experimentMethod", chosenMethod.getName()), v(DataDogConstants.DATADOG_EXPERIMENTID_KEY, id));
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
                        completableFuture.complete(false);
                        return null;
                    }
                    startTime = Instant.now();
                    experimentState = ExperimentState.STARTED;
                } else {
                    completableFuture.complete(false);
                    return null;
                }
                completableFuture.complete(true);
                return null;
            } finally {
                existingMDC.keySet().forEach(MDC::remove);
            }
        });
        return completableFuture;
    }

    public ExperimentType getExperimentType () {
        return experimentType;
    }

    ExperimentState getExperimentState () {
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
        }catch(Exception e){
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
                    if(selfHealingCounter.get()<=DEFAULT_MAXIMUM_SELF_HEALING_RETRIES) {
                        StringBuilder message = new StringBuilder();
                        message.append(ExperimentConstants.THE_EXPERIMENT_HAS_GONE_ON_TOO_LONG_INVOKING_SELF_HEALING);
                        NotificationLevel notificationLevel = NotificationLevel.ERROR;
                        if (selfHealingCounter.incrementAndGet() > 1) {
                            message.append(ExperimentConstants.THIS_IS_SELF_HEALING_ATTEMPT_NUMBER)
                                   .append(selfHealingCounter.get())
                                   .append(".");
                            notificationLevel=NotificationLevel.WARN;
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

    protected boolean isOverDuration () {
        return Instant.now().isAfter(getStartTime().plus(maximumDuration));
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
            throw new ChaosException("Exception while self healing.", e);
        } finally {
            lastSelfHealingTime = Instant.now();
        }
    }

    public Instant getStartTime () {
        return startTime;
    }

    private Duration getMinimumTimeBetweenSelfHealing () {
        return container.getMinimumSelfHealingInterval();
    }

    protected Duration getDuration () {
        return Duration.between(getStartTime(), getEndTime());
    }

    private Instant getEndTime () {
        return Optional.ofNullable(endTime).orElse(Instant.now());
    }

    private void setEndTime (Instant endTime) {
        this.endTime = endTime;
    }
}
