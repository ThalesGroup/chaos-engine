package com.gemalto.chaos.attack;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;

public abstract class Attack {
    private static final Logger log = LoggerFactory.getLogger(Attack.class);
    private final String id = randomUUID().toString();
    protected Container container;
    protected AttackType attackType;
    protected Integer timeToLive = 1;
    protected Duration duration = Duration.ofMinutes(AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES);
    private AtomicInteger timeToLiveCounter = new AtomicInteger(1);
    private AttackState attackState = AttackState.NOT_YET_STARTED;
    private transient NotificationManager notificationManager;
    private Callable<Void> selfHealingMethod;
    private Instant startTime = Instant.now();

    String getId () {
        return id;
    }

    public Instant getStartTime () {
        return startTime;
    }

    public Container getContainer () {
        return container;
    }

    public AttackType getAttackType () {
        return attackType;
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
            startAttackImpl(attackType);
            attackState = AttackState.STARTED;
            notificationManager.sendNotification(ChaosEvent.builder()
                                                           .fromAttack(this)
                                                           .withNotificationLevel(NotificationLevel.WARN)
                                                           .withMessage("This is a new attack, with " + timeToLive + " total attacks.")
                                                           .build());
        }
        return true;
    }

    private void startAttackImpl (AttackType attackType) {
        selfHealingMethod = container.attackContainer(attackType);
    }

    AttackState getAttackState () {
        attackState = checkAttackState();
        return attackState;
    }

    private AttackState checkAttackState () {
        if (isOverDuration()) {
            try {
                log.info("This attack has gone on too long, invoking self-healing. \n{}", this);
                selfHealingMethod.call();
            } catch (Exception e) {
                throw new ChaosException("An exception occurred while self-healing", e);
            }
        }
        switch (container.getContainerHealth(attackType)) {
            case NORMAL:
                if (checkTimeToLive()) {
                    log.info("Attack {} complete", this);
                    notificationManager.sendNotification(ChaosEvent.builder()
                                                                   .fromAttack(this)
                                                                   .withNotificationLevel(NotificationLevel.GOOD)
                                                                   .withMessage("Container recovered from final attack")
                                                                   .build());
                    return AttackState.FINISHED;
                } else {
                    resumeAttack();
                }
                return AttackState.STARTED;
            case DOES_NOT_EXIST:
                log.info("Attack {} no longer maps to existing container", this);
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
                                                               .withMessage("Attack " + timeToLiveCounter.get() + " not yet finished.")
                                                               .build());
                return AttackState.STARTED;
        }
    }

    private boolean isOverDuration () {
        return Instant.now().isAfter(startTime.plus(duration));
    }

    private boolean checkTimeToLive () {
        return timeToLiveCounter.incrementAndGet() > timeToLive;
    }

    private void resumeAttack () {
        if (!AdminManager.canRunAttacks()) return;
        container.repeatAttack();
    }

    public Integer getTimeToLiveCounter () {
        return timeToLiveCounter.get();
    }

    public Integer getTimetoLive () {
        return timeToLive;
    }
}
