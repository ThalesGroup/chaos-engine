package com.gemalto.chaos.attack;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AttackManager {

    private Set<Attack> activeAttacks = new HashSet<>();

    public void addAttack(Attack attack) {
        activeAttacks.add(attack);
        attack.startAttack();
    }

}
