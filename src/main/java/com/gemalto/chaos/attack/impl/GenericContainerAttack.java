package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;

import java.time.Duration;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES;
import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS;

public class GenericContainerAttack extends Attack {
    public static GenericContainerAttackBuilder builder () {
        return GenericContainerAttackBuilder.builder();
    }

    public static final class GenericContainerAttackBuilder {
        protected Container container;
        private AttackType attackType;
        private Duration duration = Duration.ofMinutes(DEFAULT_ATTACK_DURATION_MINUTES);
        private Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
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

        public GenericContainerAttackBuilder withDuration (Duration duration) {
            this.duration = duration;
            return this;
        }

        public GenericContainerAttackBuilder withFinalzationDuration (Duration finalizationDuration) {
            this.finalizationDuration = finalizationDuration;
            return this;
        }

        public GenericContainerAttack build () {
            GenericContainerAttack genericContainerAttack = new GenericContainerAttack();
            genericContainerAttack.attackType = this.attackType;
            genericContainerAttack.container = this.container;
            genericContainerAttack.duration = this.duration;
            genericContainerAttack.finalizationDuration = this.finalizationDuration;
            return genericContainerAttack;
        }
    }
}
