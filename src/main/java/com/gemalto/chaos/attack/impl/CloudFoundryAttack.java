package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;

import java.time.Duration;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES;

public class CloudFoundryAttack extends Attack {
    public static CloudFoundryAttackBuilder builder () {
        return CloudFoundryAttackBuilder.builder();
    }

    public static final class CloudFoundryAttackBuilder {
        private Container container;
        private AttackType attackType;
        private Integer timeToLive;
        private Duration duration = Duration.ofMinutes(DEFAULT_ATTACK_DURATION_MINUTES);

        private CloudFoundryAttackBuilder () {
        }

        static CloudFoundryAttackBuilder builder () {
            return new CloudFoundryAttackBuilder();
        }

        public CloudFoundryAttackBuilder container (Container container) {
            this.container = container;
            return this;
        }

        public CloudFoundryAttackBuilder attackType (AttackType attackType) {
            this.attackType = attackType;
            return this;
        }

        public CloudFoundryAttackBuilder timeToLive (Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public CloudFoundryAttackBuilder duration (Duration duration) {
            this.duration = duration;
            return this;
        }

        public CloudFoundryAttack build () {
            CloudFoundryAttack cloudFoundryAttack = new CloudFoundryAttack();
            cloudFoundryAttack.attackType = this.attackType;
            cloudFoundryAttack.container = this.container;
            cloudFoundryAttack.timeToLive += this.timeToLive;
            cloudFoundryAttack.duration = this.duration;
            return cloudFoundryAttack;
        }
    }
}
