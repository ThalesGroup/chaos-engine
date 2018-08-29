package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.NetworkAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gemalto.chaos.constants.AwsEC2Constants.AWS_EC2_HARD_REBOOT_TIMER_MINUTES;

public class AwsEC2Container extends Container {
    private String instanceId;
    private String keyName;
    private String name;
    private transient AwsEC2Platform awsEC2Platform;
    private final transient Callable<Void> startContainerMethod = () -> {
        awsEC2Platform.startInstance(instanceId);
        return null;
    };
    private final transient Callable<ContainerHealth> checkContainerStartedMethod = () -> awsEC2Platform.checkHealth(instanceId);

    private AwsEC2Container () {
        super();
    }

    public static AwsEC2ContainerBuilder builder () {
        return AwsEC2ContainerBuilder.anAwsEC2Container();
    }

    @Override
    public Platform getPlatform () {
        return awsEC2Platform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return awsEC2Platform.checkHealth(instanceId);
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return GenericContainerAttack.builder().withAttackType(attackType).withContainer(this).build();
    }

    @Override
    public String getSimpleName () {
        return String.format("%s (%s) [%s]", name, keyName, instanceId);
    }

    @StateAttack
    public void stopContainer (Attack attack) {
        awsEC2Platform.stopInstance(instanceId);
        attack.setSelfHealingMethod(startContainerMethod);
        attack.setCheckContainerHealth(checkContainerStartedMethod);
    }

    @StateAttack
    public void restartContainer (Attack attack) {
        final Instant hardRebootTimer = Instant.now().plus(Duration.ofMinutes(AWS_EC2_HARD_REBOOT_TIMER_MINUTES));
        awsEC2Platform.restartInstance(instanceId);
        attack.setSelfHealingMethod(startContainerMethod);
        attack.setCheckContainerHealth(() -> hardRebootTimer.isBefore(Instant.now()) ? awsEC2Platform.checkHealth(instanceId) : ContainerHealth.UNDER_ATTACK);
        // If Ctrl+Alt+Del is disabled in the AMI, then it takes 4 minutes for EC2 to initiate a hard reboot.
    }

    @NetworkAttack
    public void removeSecurityGroups (Attack attack) {
        List<String> originalSecurityGroupIds = awsEC2Platform.getSecurityGroupIds(instanceId);
        awsEC2Platform.setSecurityGroupIds(instanceId, Collections.singletonList(awsEC2Platform.getChaosSecurityGroupId()));
        attack.setCheckContainerHealth(() -> awsEC2Platform.verifySecurityGroupIds(instanceId, originalSecurityGroupIds));
        attack.setSelfHealingMethod(() -> {
            awsEC2Platform.setSecurityGroupIds(instanceId, originalSecurityGroupIds);
            return null;
        });
    }

    public static final class AwsEC2ContainerBuilder {
        private String instanceId;
        private String keyName;
        private String name;
        private AwsEC2Platform awsEC2Platform;

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

        public AwsEC2ContainerBuilder awsEC2Platform (AwsEC2Platform awsEC2Platform) {
            this.awsEC2Platform = awsEC2Platform;
            return this;
        }

        public AwsEC2Container build () {
            AwsEC2Container awsEC2Container = new AwsEC2Container();
            awsEC2Container.awsEC2Platform = this.awsEC2Platform;
            awsEC2Container.instanceId = this.instanceId;
            awsEC2Container.keyName = this.keyName;
            awsEC2Container.name = this.name;
            return awsEC2Container;
        }
    }
}
