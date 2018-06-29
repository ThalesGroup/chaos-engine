package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsPlatform;

public class AwsEC2Container extends Container {
    private String instanceId;
    private AwsPlatform awsPlatform;

    public static AwsEC2Container.Builder builder () {
        return new AwsEC2Container.Builder();
    }

    @Override
    protected Platform getPlatform () {
        return awsPlatform;
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
    public void attackContainerState () {
    }

    @Override
    public void attackContainerNetwork () {
    }

    @Override
    public void attackContainerResources () {
    }

    @Override
    public String getSimpleName () {
        return null;
    }

    public static class Builder {
        Builder () {
        }

        public AwsEC2Container build () {
            return new AwsEC2Container();
        }
    }
}
