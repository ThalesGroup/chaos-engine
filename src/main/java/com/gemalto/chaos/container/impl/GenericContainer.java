package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;

public class GenericContainer extends Container {
    @Override
    public void updateContainerHealth() {
        // Nothing to do with a fake container.
    }

    @Override
    public Attack createAttack(AttackType attackType) {
        return null;
    }
}
