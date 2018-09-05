package com.gemalto.chaos.attack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.constants.AttackConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS;
import static com.gemalto.chaos.util.MethodUtils.getMethodsWithAnnotation;
import static java.util.UUID.randomUUID;

public abstract class Attack {
    private static final Logger log = LoggerFactory.getLogger(Attack.class);
    private final String id = randomUUID().toString();
    protected Container container;
    protected AttackType attackType;
    protected Duration duration = Duration.ofMinutes(AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES);
    protected Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
    private Platform attackLayer;
    private Method attackMethod;
    private AttackState attackState = AttackState.NOT_YET_STARTED;
    private transient NotificationManager notificationManager;
    private Callable<Void> selfHealingMethod = () -> null;
    private Callable<ContainerHealth> checkContainerHealth;
    private Callable<Void> finalizeMethod;
    private Instant startTime = Instant.now();
    private Instant finalizationStartTime;

    public Platform getAttackLayer () {
        return attackLayer;
    }

    private void setAttackLayer (Platform attackLayer) {
        this.attackLayer = attackLayer;
    }

    @JsonIgnore
    public Method getAttackMethod () {
        return attackMethod;
    }

    private void setAttackMethod (Method attackMethod) {
        this.attackMethod = attackMethod;
    }

    public Callable<Void> getSelfHealingMethod () {
        return selfHealingMethod;
    }

    public void setSelfHealingMethod (Callable<Void> selfHealingMethod) {
        this.selfHealingMethod = selfHealingMethod;
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

    public Instant getStartTime () {
        return startTime;
    }

    public Container getContainer () {
        return container;
    }

    boolean startAttack (NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        if (!AdminManager.canRunAttacks()) {
            log.info("Cannot start attacks right now, system is {}", AdminManager.getAdminState());
            return false;
        }
        if (container.getContainerHealth(attackType) != ContainerHealth.NORMAL) {
            log.info("Failed to start an attack as this container is already in an abnormal state\n{}", container);
            return false;
        }
        if (container.supportsAttackType(attackType)) {
            List<Method> attackMethods = getMethodsWithAnnotation(container.getClass(), getAttackType().getAnnotation());
            if (attackMethods.isEmpty()) {
                throw new ChaosException("Could not find an attack vector");
            }
            int index = ThreadLocalRandom.current().nextInt(attackMethods.size());
            setAttackMethod(attackMethods.get(index));
            setAttackLayer(container.getPlatform());
            notificationManager.sendNotification(ChaosEvent.builder()
                                                           .fromAttack(this)
                                                           .withNotificationLevel(NotificationLevel.WARN)
                                                           .withMessage("Starting a new attack")
                                                           .build());
            container.attackContainer(this);
            attackState = AttackState.STARTED;
        }
        return true;
    }

    public AttackType getAttackType () {
        return attackType;
    }

    AttackState getAttackState () {
        attackState = checkAttackState();
        return attackState;
    }

    private synchronized AttackState checkAttackState () {
        if (isOverDuration()) {
            try {
                log.warn("The attack {} has gone on too long, invoking self-healing. \n{}", id, this);
                selfHealingMethod.call();
                notificationManager.sendNotification(ChaosEvent.builder()
                                                               .fromAttack(this)
                                                               .withNotificationLevel(NotificationLevel.WARN)
                                                               .withMessage("The attack has gone on too long, invoking self-healing.")
                                                               .build());
            } catch (Exception e) {
                log.error("Attack {}: An exception occurred while running self-healing.", id, e);
                notificationManager.sendNotification(ChaosEvent.builder()
                                                               .fromAttack(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("An exception occurred while running self-healing.")
                                                               .build());
            }
        }
        switch (checkContainerHealth()) {
            case NORMAL:
                if (isFinalizable()) {
                    log.info("Attack {} complete", id);
                    notificationManager.sendNotification(ChaosEvent.builder()
                                                                   .fromAttack(this)
                                                                   .withNotificationLevel(NotificationLevel.GOOD)
                                                                   .withMessage("Attack finished. Container recovered from the attack")
                                                                   .build());
                    finalizeAttack();
                    return AttackState.FINISHED;
                }
                return AttackState.STARTED;
            case DOES_NOT_EXIST:
                log.info("Attack {} no longer maps to existing container", id);
                notificationManager.sendNotification(ChaosEvent.builder()
                                                               .fromAttack(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Container no longer exists.")
                                                               .build());
                return AttackState.FINISHED;
            case UNDER_ATTACK:
            default:
                notificationManager.sendNotification(ChaosEvent.builder()
                                                               .fromAttack(this)
                                                               .withNotificationLevel(NotificationLevel.ERROR)
                                                               .withMessage("Attack not yet finished.")
                                                               .build());
                return AttackState.STARTED;
        }
    }

    private boolean isOverDuration () {
        return Instant.now().isAfter(startTime.plus(duration));
    }

    private ContainerHealth checkContainerHealth () {
        if (checkContainerHealth != null) {
            try {
                return checkContainerHealth.call();
            } catch (Exception e) {
                log.error("Issue while checking container health using specific method", e);
            }
        }
        return container.getContainerHealth(attackType);
    }

    private boolean isFinalizable () {
        if (finalizationStartTime == null) {
            finalizationStartTime = Instant.now();
        }
        boolean finalizable = Instant.now().isAfter(finalizationStartTime.plus(finalizationDuration));
        log.debug("Attack {} is finalizable = {}", id, finalizable);
        return finalizable;
    }

    private void finalizeAttack () {
        if (finalizeMethod != null) {
            try {
                log.info("Finalizing attack {} on container {}", id, container.getSimpleName());
                finalizeMethod.call();
            } catch (Exception e) {
                log.error("Error while finalizing attack {} on container {}", id, container.getSimpleName());
            }
        }
    }
}
