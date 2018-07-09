package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;

import java.time.Duration;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES;

public class GenericContainerAttack extends Attack {
    public static GenericContainerAttackBuilder builder () {
        return GenericContainerAttackBuilder.builder();
    }

    public static final class GenericContainerAttackBuilder {
        protected Container container;
        private AttackType attackType;
        private Integer timeToLive = 1;
        private Duration duration = Duration.ofMinutes(DEFAULT_ATTACK_DURATION_MINUTES);

        private GenericContainerAttackBuilder () {
        }

        public static GenericContainerAttackBuilder builder () {
            return new GenericContainerAttackBuilder();
        }

        public GenericContainerAttackBuilder withContainer (Container container) {
            this.container = container;
            return this;
        }

        public GenericContainerAttackBuilder withAttackType (AttackType attackType) {
            this.attackType = attackType;
            return this;
        }

        public GenericContainerAttackBuilder withTimeToLive (Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public GenericContainerAttackBuilder withDuration (Duration duration) {
            this.duration = duration;
            return this;
        }

        public GenericContainerAttack build () {
            GenericContainerAttack genericContainerAttack = new GenericContainerAttack();
            genericContainerAttack.attackType = this.attackType;
            genericContainerAttack.timeToLive = this.timeToLive;
            genericContainerAttack.container = this.container;
            genericContainerAttack.duration = this.duration;
            return genericContainerAttack;
        }
    }
}
