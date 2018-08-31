package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;

public class AwsRDSInstanceContainer extends Container {
    private String dbInstanceIdentifier;

    public String getDbInstanceIdentifier () {
        return dbInstanceIdentifier;
    }

    @Override
    public Platform getPlatform () {
        return null;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return null;
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return null;
    }

    @Override
    public String getSimpleName () {
        return null;
    }
}
