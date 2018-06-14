package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.services.CloudService;

import java.util.Date;

public abstract class Attack {
    protected Container container;
    protected AttackType attackType;
    private AttackState attackState = AttackState.NOT_YET_STARTED;

    public abstract CloudService getCloudService();

    public void stopContainer() {
        getCloudService().kill(container);
    }

    protected void startAttack() {
        if (container.supportsAttackType(attackType)) {
            startAttack_impl(container, attackType);
            attackState = AttackState.STARTED;
            NotificationManager.sendNotification(
                    ChaosEvent.builder()
                            .withTargetContainer(container)
                            .withChaosTime(new Date())
                            .build()
            );
        }

    }

    protected abstract void startAttack_impl(Container container, AttackType attackType);

}
