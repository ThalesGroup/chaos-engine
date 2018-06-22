package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudFoundryAttack extends Attack {
    @Autowired
    private CloudFoundryPlatform cloudFoundryPlatform;

    public static CloudFoundryAttackBuilder builder () {
        return CloudFoundryAttackBuilder.builder();
    }

    @Override
    public Platform getPlatform () {
        return cloudFoundryPlatform;
    }

    public static final class CloudFoundryAttackBuilder {
        private Container container;
        private AttackType attackType;
        private Integer timeToLive;

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

        public CloudFoundryAttack build () {
            CloudFoundryAttack cloudFoundryAttack = new CloudFoundryAttack();
            cloudFoundryAttack.attackType = this.attackType;
            cloudFoundryAttack.container = this.container;
            cloudFoundryAttack.timeToLive += this.timeToLive;
            return cloudFoundryAttack;
        }
    }
}
