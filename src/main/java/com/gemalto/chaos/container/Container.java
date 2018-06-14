package com.gemalto.chaos.container;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;

import java.util.HashSet;
import java.util.Set;

public abstract class Container {

    protected static Set<AttackType> supportedAttackTypes = new HashSet<>();
    protected ContainerHealth containerHealth;

    public boolean supportsAttackType(AttackType attackType) {
        return supportedAttackTypes != null && supportedAttackTypes.contains(attackType);
    }

    public abstract void updateContainerHealth();

    public ContainerHealth getContainerHealth() {
        updateContainerHealth();
        return containerHealth;
    }
}
