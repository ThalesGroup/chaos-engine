package com.gemalto.chaos.attack;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class Attack {
    protected Container container;
    protected AttackType attackType;
    private AttackState attackState = AttackState.NOT_YET_STARTED;

    private static final Logger log = LoggerFactory.getLogger(Attack.class);

    public abstract Platform getPlatform();

    void startAttack() {
        if (!AdminManager.canRunAttacks()) {
            log.info("Cannot start attacks right now, system is {}", AdminManager.getAdminState());
        }
        if (container.supportsAttackType(attackType)) {
            startAttackImpl(container, attackType);
            attackState = AttackState.STARTED;
            NotificationManager.sendNotification(
                    ChaosEvent.builder()
                            .withTargetContainer(container)
                            .withChaosTime(new Date())
                            .build()
            );
        }

    }

    private void startAttackImpl(Container container, AttackType attackType) {
        container.attackContainer(attackType);
    }

    AttackState getAttackState() {
        attackState = checkAttackState();
        return attackState;
    }

    protected abstract AttackState checkAttackState();
}
