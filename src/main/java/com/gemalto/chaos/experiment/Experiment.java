package com.gemalto.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.AdminManager;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gemalto.chaos.constants.ExperimentConstants.DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS;
import static com.gemalto.chaos.util.MethodUtils.getMethodsWithAnnotation;
import static java.util.UUID.randomUUID;

public abstract class Experiment {
    private static final Logger log = LoggerFactory.getLogger(Experiment.class);
    private final String id = randomUUID().toString();
    protected Container container;
    protected ExperimentType experimentType;
    protected Duration duration = Duration.ofMinutes(ExperimentConstants.DEFAULT_EXPERIMENT_DURATION_MINUTES);
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
    private Instant finalizationStartTime;
    private Instant lastSelfHealingTime;
    private AtomicInteger selfHealingCounter = new AtomicInteger(0);

    NotificationManager getNotificationManager () {
        return notificationManager;
    }

    public void setNotificationManager (NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public Instant getLastSelfHealingTime () {
        return lastSelfHealingTime;
    }

    public Platform getExperimentLayer () {
        return experimentLayer;
    }

    private void setExperimentLayer (Platform experimentLayer) {
        this.experimentLayer = experimentLayer;
    }

    @JsonIgnore
    public Method getExperimentMethod () {
        return experimentMethod;
    }

    private void setExperimentMethod (Method experimentMethod) {
        this.experimentMethod = experimentMethod;
    }

    public Callable<Void> getSelfHealingMethod () {
        return selfHealingMethod;
    }

    public void setSelfHealingMethod (Callable<Void> selfHealingMethod) {
        this.selfHealingMethod = selfHealingMethod;
    }

    public Callable<Void> getFinalizeMethod () {
        return finalizeMethod;
    }

    public void setFinalizeMethod (Callable<Void> finalizeMethod) {
        this.finalizeMethod = finalizeMethod;
    }

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

    boolean startExperiment () {
        if (!adminManager.canRunExperiments()) {
            log.info("Cannot start experiments right now, system is {}", adminManager.getAdminState());
            return false;
        }
        if (container.getContainerHealth(experimentType) != ContainerHealth.NORMAL) {
            log.info("Failed to start an experiment as this container is already in an abnormal state\n{}", container);
            return false;
        }
        if (container.supportsExperimentType(experimentType)) {
            List<Method> experimentMethods = getMethodsWithAnnotation(container.getClass(), getExperimentType().getAnnotation());
            if (experimentMethods.isEmpty()) {
                throw new ChaosException("Could not find an experiment vector");
            }
            int index = ThreadLocalRandom.current().nextInt(experimentMethods.size());
            setExperimentMethod(experimentMethods.get(index));
            setExperimentLayer(container.getPlatform());
            notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                           .withNotificationLevel(NotificationLevel.WARN)
                                                           .withMessage("Starting new experiment")
                                                           .build());
            try{
                container.startExperiment(this);
            }catch (ChaosException ex){
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Failed to start experiment "+ex.getMessage())
                                                               .build());
                return false;
            }
            experimentState = ExperimentState.STARTED;
        } else return false;
        return true;
    }

    public ExperimentType getExperimentType () {
        return experimentType;
    }

    ExperimentState getExperimentState () {
        experimentState = checkExperimentState();
        return experimentState;
    }

    private synchronized ExperimentState checkExperimentState () {
        switch (checkContainerHealth()) {
            case NORMAL:
                if (isFinalizable()) {
                    log.info("Experiment {} complete", id);
                    notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                                   .withNotificationLevel(NotificationLevel.GOOD)
                                                                   .withMessage("Experiment finished. Container recovered from the experiment")
                                                                   .build());
                    finalizeExperiment();
                    return ExperimentState.FINISHED;
                }
                return ExperimentState.STARTED;
            case DOES_NOT_EXIST:
                log.info("Experiment {} no longer maps to existing container", id);
                notificationManager.sendNotification(ChaosEvent.builder().fromExperiment(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Container no longer exists.")
                                                               .build());
                return ExperimentState.FINISHED;
            case RUNNING_EXPERIMENT:
            default:
                doSelfHealing();
                return ExperimentState.STARTED;
        }
    }

    private ContainerHealth checkContainerHealth () {
        if (checkContainerHealth != null) {
            try {
                return checkContainerHealth.call();
            } catch (Exception e) {
                log.error("Issue while checking container health using specific method", e);
            }
        }
        return container.getContainerHealth(experimentType);
    }

    public boolean isFinalizable () {
        if (getFinalizationStartTime() == null) {
            finalizationStartTime = Instant.now();
        }
        boolean finalizable = Instant.now().isAfter(getFinalizationStartTime().plus(finalizationDuration));
        log.debug("Experiment {} is finalizable = {}", id, finalizable);
        return finalizable;
    }

    private void finalizeExperiment () {
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
            }
        }
    }

    public void doSelfHealing () {
        if (isOverDuration()) {
            try {
                log.warn("The experiment {} has gone on too long, invoking self-healing. \n{}", id, this);
                ChaosEvent chaosEvent;
                if (canRunSelfHealing()) {
                    StringBuilder message = new StringBuilder();
                    message.append(ExperimentConstants.THE_EXPERIMENT_HAS_GONE_ON_TOO_LONG_INVOKING_SELF_HEALING);
                    if (selfHealingCounter.incrementAndGet() > 1) {
                        message.append(ExperimentConstants.THIS_IS_SELF_HEALING_ATTEMPT_NUMBER)
                               .append(selfHealingCounter.get())
                               .append(".");
                    }
                    chaosEvent = ChaosEvent.builder().fromExperiment(this)
                                           .withNotificationLevel(NotificationLevel.WARN)
                                           .withMessage(message.toString())
                                           .build();
                    callSelfHealing();
                } else if (adminManager.canRunSelfHealing()) {
                    chaosEvent = ChaosEvent.builder().fromExperiment(this)
                                           .withNotificationLevel(NotificationLevel.WARN)
                                           .withMessage(ExperimentConstants.CANNOT_RUN_SELF_HEALING_AGAIN_YET)
                                           .build();
                } else {
                    chaosEvent = ChaosEvent.builder().fromExperiment(this)
                                           .withNotificationLevel(NotificationLevel.WARN)
                                           .withMessage(ExperimentConstants.SYSTEM_IS_PAUSED_AND_UNABLE_TO_RUN_SELF_HEALING)
                                           .build();
                }
                notificationManager.sendNotification(chaosEvent);
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
    }

    public Instant getFinalizationStartTime () {
        return finalizationStartTime;
    }

    protected boolean isOverDuration () {
        return Instant.now().isAfter(getStartTime().plus(duration));
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
}
