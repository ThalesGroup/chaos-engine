package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.AwsEC2Attack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsPlatform;

import java.util.Arrays;

public class AwsEC2Container extends Container {
    private String instanceId;
    private String keyName;
    private String name;
    private transient AwsPlatform awsPlatform;

    private AwsEC2Container () {
        supportedAttackTypes.addAll(Arrays.asList(AttackType.STATE));
    }

    @Override
    protected Platform getPlatform () {
        return awsPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return awsPlatform.checkHealth(instanceId);
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return AwsEC2Attack.builder().withAttackType(attackType).withContainer(this).withTimeToLive(1).build();
    }

    @Override
    public void attackContainerState () {
        awsPlatform.stopInstance(instanceId);
    }

    @Override
    public void attackContainerNetwork () {
    }

    @Override
    public void attackContainerResources () {
    }

    @Override
    public String getSimpleName () {
        return String.format("%s (%s) [%s]", name, keyName, instanceId);
    }

    public static final class AwsEC2ContainerBuilder {
        private FateManager fateManager;
        private String instanceId;
        private String keyName;
        private String name;
        private AwsPlatform awsPlatform;

        private AwsEC2ContainerBuilder () {
        }

        public static AwsEC2ContainerBuilder anAwsEC2Container () {
            return new AwsEC2ContainerBuilder();
        }

        public AwsEC2ContainerBuilder instanceId (String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public AwsEC2ContainerBuilder keyName (String keyName) {
            this.keyName = keyName;
            return this;
        }

        public AwsEC2ContainerBuilder name (String name) {
            this.name = name;
            return this;
        }

        public AwsEC2ContainerBuilder awsPlatform (AwsPlatform awsPlatform) {
            this.awsPlatform = awsPlatform;
            return this;
        }

        public AwsEC2ContainerBuilder fateManager (FateManager fateManager) {
            this.fateManager = fateManager;
            return this;
        }

        public AwsEC2Container build () {
            AwsEC2Container awsEC2Container = new AwsEC2Container();
            awsEC2Container.awsPlatform = this.awsPlatform;
            awsEC2Container.fateManager = this.fateManager;
            awsEC2Container.instanceId = this.instanceId;
            awsEC2Container.keyName = this.keyName;
            awsEC2Container.name = this.name;
            return awsEC2Container;
        }
    }
}
