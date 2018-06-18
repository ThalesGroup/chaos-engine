package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.GenericPlatform;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericContainer extends Container {

    @Autowired
    private GenericPlatform genericPlatform;


    @Override
    public void updateContainerHealth() {
        // Nothing to do with a fake container.
    }

    @Override
    public Attack createAttack(AttackType attackType) {
        return null;
    }

    @Override
    protected Platform getPlatform() {
        return genericPlatform;
    }
}
