package com.gemalto.chaos.attack.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.services.CloudService;
import com.gemalto.chaos.services.impl.CloudFoundryService;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudFoundryAttack extends Attack {

    @Autowired
    private CloudFoundryService cloudFoundryService;

    public static CloudFoundryAttackBuilder builder() {
        return CloudFoundryAttackBuilder.builder();
    }

    @Override
    public CloudService getCloudService() {
        return cloudFoundryService;
    }

    @Override
    protected void startAttackImpl(Container container, AttackType attackType) {
        container.startAttack();
        getCloudService().kill(container);
    }

    @Override
    protected void checkAttackState() {
        // TODO: Use CF API to check if App Instance is well again.
    }

    public static final class CloudFoundryAttackBuilder {
        private Container container;
        private AttackType attackType;

        private CloudFoundryAttackBuilder() {
        }

        static CloudFoundryAttackBuilder builder() {
            return new CloudFoundryAttackBuilder();
        }

        public CloudFoundryAttackBuilder container(Container container) {
            this.container = container;
            return this;
        }

        public CloudFoundryAttackBuilder attackType(AttackType attackType) {
            this.attackType = attackType;
            return this;
        }

        public CloudFoundryAttack build() {
            CloudFoundryAttack cloudFoundryAttack = new CloudFoundryAttack();
            cloudFoundryAttack.attackType = this.attackType;
            cloudFoundryAttack.container = this.container;
            return cloudFoundryAttack;
        }
    }
}
