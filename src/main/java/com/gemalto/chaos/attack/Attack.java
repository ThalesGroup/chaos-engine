package com.gemalto.chaos.attack;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Attack {
    private static final Logger log = LoggerFactory.getLogger(Attack.class);
    protected Container container;
    protected AttackType attackType;
    protected Integer timeToLive = 1;
    private AtomicInteger timeToLiveCounter = new AtomicInteger(1);
    private AttackState attackState = AttackState.NOT_YET_STARTED;
    private NotificationManager notificationManager;

    public abstract Platform getPlatform ();

    void startAttack (NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        if (!AdminManager.canRunAttacks()) {
            log.info("Cannot start attacks right now, system is {}", AdminManager.getAdminState());
            return;
        }
        if (container.supportsAttackType(attackType)) {
            startAttackImpl(container, attackType);
            attackState = AttackState.STARTED;
            notificationManager.sendNotification(ChaosEvent.builder()
                                                           .withTargetContainer(container)
                                                           .withAttackType(attackType)
                                                           .withChaosTime(new Date())
                                                           .withNotificationLevel(NotificationLevel.WARN)
                                                           .withMessage("This is a new attack, with " + timeToLive + " total attacks.")
                                                           .build());
        }
    }

    private void startAttackImpl (Container container, AttackType attackType) {
        container.attackContainer(attackType);
    }

    private AttackState checkAttackState () {
        if (container.getContainerHealth(attackType) == ContainerHealth.NORMAL) {
            if (checkTimeToLive()) {
                log.info("Attack {} complete", this);
                notificationManager.sendNotification(ChaosEvent.builder()
                                                               .withTargetContainer(container)
                                                               .withAttackType(attackType)
                                                               .withChaosTime(new Date())
                                                               .withNotificationLevel(NotificationLevel.GOOD)
                                                               .withMessage("Container recovered from final attack")
                                                               .build());
                return AttackState.FINISHED;
            } else {
                resumeAttack();
            }
        }
        notificationManager.sendNotification(ChaosEvent.builder()
                                                       .withTargetContainer(container)
                                                       .withAttackType(attackType)
                                                       .withChaosTime(new Date())
                                                       .withNotificationLevel(NotificationLevel.ERROR)
                                                       .withMessage("Attack " + timeToLiveCounter.get() + " not yet finished.")
                                                       .build());
        return AttackState.STARTED;
    }

    private boolean checkTimeToLive () {
        return timeToLiveCounter.incrementAndGet() > timeToLive;
    }

    AttackState getAttackState () {
        attackState = checkAttackState();
        return attackState;
    }

    private void resumeAttack () {
        if (!AdminManager.canRunAttacks()) return;
        startAttackImpl(container, attackType);
    }
}
