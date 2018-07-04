package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;

import java.time.Duration;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES;

public class AwsEC2Attack extends Attack {
    public static AwsEC2AttackBuilder builder () {
        return new AwsEC2AttackBuilder();
    }

    public static final class AwsEC2AttackBuilder {
        protected Container container;
        private AttackType attackType;
        private Integer timeToLive = 1;
        private Duration duration = Duration.ofMinutes(DEFAULT_ATTACK_DURATION_MINUTES);

        private AwsEC2AttackBuilder () {
        }

        public static AwsEC2AttackBuilder anAwsEC2Attack () {
            return new AwsEC2AttackBuilder();
        }

        public AwsEC2AttackBuilder withContainer (Container container) {
            this.container = container;
            return this;
        }

        public AwsEC2AttackBuilder withAttackType (AttackType attackType) {
            this.attackType = attackType;
            return this;
        }

        public AwsEC2AttackBuilder withTimeToLive (Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public AwsEC2AttackBuilder withDuration (Duration duration) {
            this.duration = duration;
            return this;
        }

        public AwsEC2Attack build () {
            AwsEC2Attack awsEC2Attack = new AwsEC2Attack();
            awsEC2Attack.attackType = this.attackType;
            awsEC2Attack.timeToLive = this.timeToLive;
            awsEC2Attack.container = this.container;
            awsEC2Attack.duration = this.duration;
            return awsEC2Attack;
        }
    }
}
