package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AttackManager {
    private static final Logger log = LoggerFactory.getLogger(AttackManager.class);
    private Set<Attack> activeAttacks = new HashSet<>();
    @Autowired
    private NotificationManager notificationManager;

    public void addAttack (Attack attack) {
        activeAttacks.add(attack);
        attack.startAttack(notificationManager);
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 15 * 1000)
    public void updateAttackStatus () {
        log.debug("Checking on existing attacks");
        if (activeAttacks != null && !activeAttacks.isEmpty()) {
            updateAttackStatusImpl();
        } else {
            log.debug("No attacks are currently active.");
        }
    }

    private void updateAttackStatusImpl () {
        log.info("Updating status on active attacks");
        Set<Attack> finishedAttacks = new HashSet<>();
        for (Attack attack : activeAttacks) {
            AttackState attackState = attack.getAttackState();
            if (attackState == AttackState.FINISHED) {
                finishedAttacks.add(attack);
            }
        }
        activeAttacks.removeAll(finishedAttacks);
    }
}
