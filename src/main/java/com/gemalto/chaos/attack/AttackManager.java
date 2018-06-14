package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AttackManager {

    private static final Logger log = LoggerFactory.getLogger(AttackManager.class);

    private Set<Attack> activeAttacks = new HashSet<>();

    public void addAttack(Attack attack) {
        activeAttacks.add(attack);
        attack.startAttack();
    }


    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    public void updateAttackStatus() {
        log.info("Checking on existing attacks");

        if (activeAttacks != null && !activeAttacks.isEmpty()) {
            updateAttackStatusImpl();
        } else {
            log.info("No attacks are currently active.");
        }

    }

    private void updateAttackStatusImpl() {
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
