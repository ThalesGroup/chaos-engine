package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.NetworkAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;

import java.util.Collection;

public class AwsRDSInstanceContainer extends AwsContainer {
    private String dbInstanceIdentifier;
    private String engine;
    private transient AwsRDSPlatform awsRDSPlatform;

    public String getDbInstanceIdentifier () {
        return dbInstanceIdentifier;
    }

    @Override
    public Platform getPlatform () {
        return awsRDSPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier);
    }

    @Override
    public String getSimpleName () {
        return getDbInstanceIdentifier();
    }

    @StateAttack
    public void restartInstance (Attack attack) {
        attack.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier));
        awsRDSPlatform.restartInstance(dbInstanceIdentifier);
    }

    @NetworkAttack
    public void removeSecurityGroups (Attack attack) {
        Collection<String> existingSecurityGroups = awsRDSPlatform.getVpcSecurityGroupIds(dbInstanceIdentifier);
        attack.setSelfHealingMethod(() -> {
            awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, existingSecurityGroups);
            return null;
        });
        attack.setCheckContainerHealth(() -> awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, existingSecurityGroups));
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, awsRDSPlatform.getChaosSecurityGroup());
    }

    public static AwsRDSInstanceContainerBuilder builder () {
        return AwsRDSInstanceContainerBuilder.anAwsRDSInstanceContainer();
    }

    public static final class AwsRDSInstanceContainerBuilder {
        private String dbInstanceIdentifier;
        private String engine;
        private String availabilityZone;
        private AwsRDSPlatform awsRDSPlatform;

        private AwsRDSInstanceContainerBuilder () {
        }

        static AwsRDSInstanceContainerBuilder anAwsRDSInstanceContainer () {
            return new AwsRDSInstanceContainerBuilder();
        }

        public AwsRDSInstanceContainerBuilder withDbInstanceIdentifier (String dbInstanceIdentifier) {
            this.dbInstanceIdentifier = dbInstanceIdentifier;
            return this;
        }

        public AwsRDSInstanceContainerBuilder withEngine (String engine) {
            this.engine = engine;
            return this;
        }

        public AwsRDSInstanceContainerBuilder withAwsRDSPlatform (AwsRDSPlatform awsRDSPlatform) {
            this.awsRDSPlatform = awsRDSPlatform;
            return this;
        }

        public AwsRDSInstanceContainerBuilder withAvailabilityZone (String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public AwsRDSInstanceContainer build () {
            AwsRDSInstanceContainer awsRDSInstanceContainer = new AwsRDSInstanceContainer();
            awsRDSInstanceContainer.dbInstanceIdentifier = this.dbInstanceIdentifier;
            awsRDSInstanceContainer.engine = this.engine;
            awsRDSInstanceContainer.awsRDSPlatform = this.awsRDSPlatform;
            awsRDSInstanceContainer.availabilityZone = this.availabilityZone;
            return awsRDSInstanceContainer;
        }
    }
}
