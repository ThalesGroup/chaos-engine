package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsPlatform;

import java.util.concurrent.Callable;

public class AwsEC2Container extends Container {
    private String instanceId;
    private String keyName;
    private String name;
    private transient AwsPlatform awsPlatform;
    private final transient Callable<Void> startContainerMethod = () -> {
        awsPlatform.startInstance(instanceId);
        return null;
    };

    private AwsEC2Container () {
        super();
    }

    public static AwsEC2ContainerBuilder builder () {
        return AwsEC2ContainerBuilder.anAwsEC2Container();
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
        return GenericContainerAttack.builder()
                                     .withAttackType(attackType)
                                     .withContainer(this)
                                     .withTimeToLive(1)
                                     .build();
    }

    @Override
    public String getSimpleName () {
        return String.format("%s (%s) [%s]", name, keyName, instanceId);
    }

    @StateAttack
    public Callable<Void> stopContainer () {
        awsPlatform.stopInstance(instanceId);
        return startContainerMethod;
    }

    public static final class AwsEC2ContainerBuilder {
        private String instanceId;
        private String keyName;
        private String name;
        private AwsPlatform awsPlatform;

        private AwsEC2ContainerBuilder () {
        }

        static AwsEC2ContainerBuilder anAwsEC2Container () {
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

        public AwsEC2Container build () {
            AwsEC2Container awsEC2Container = new AwsEC2Container();
            awsEC2Container.awsPlatform = this.awsPlatform;
            awsEC2Container.instanceId = this.instanceId;
            awsEC2Container.keyName = this.keyName;
            awsEC2Container.name = this.name;
            return awsEC2Container;
        }
    }
}
