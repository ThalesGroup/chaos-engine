package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;

public class AwsRDSClusterContainer extends Container {
    private String dbClusterIdentifier;
    private String engine;
    private transient AwsRDSPlatform awsRDSPlatform;

    public String getDbClusterIdentifier () {
        return dbClusterIdentifier;
    }

    @Override
    public Platform getPlatform () {
        return awsRDSPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        return null;
    }

    @Override
    public String getSimpleName () {
        return null;
    }

    public static AwsRDSClusterContainerBuilder builder () {
        return AwsRDSClusterContainerBuilder.anAwsRDSClusterContainer();
    }

    public static final class AwsRDSClusterContainerBuilder {
        private String dbClusterIdentifier;
        private String engine;
        private AwsRDSPlatform awsRDSPlatform;

        private AwsRDSClusterContainerBuilder () {
        }

        static AwsRDSClusterContainerBuilder anAwsRDSClusterContainer () {
            return new AwsRDSClusterContainerBuilder();
        }

        public AwsRDSClusterContainerBuilder withDbClusterIdentifier (String dbClusterIdentifier) {
            this.dbClusterIdentifier = dbClusterIdentifier;
            return this;
        }

        public AwsRDSClusterContainerBuilder withEngine (String engine) {
            this.engine = engine;
            return this;
        }

        public AwsRDSClusterContainerBuilder withAwsRDSPlatform (AwsRDSPlatform awsRDSPlatform) {
            this.awsRDSPlatform = awsRDSPlatform;
            return this;
        }

        public AwsRDSClusterContainer build () {
            AwsRDSClusterContainer awsRDSClusterContainer = new AwsRDSClusterContainer();
            awsRDSClusterContainer.engine = this.engine;
            awsRDSClusterContainer.dbClusterIdentifier = this.dbClusterIdentifier;
            awsRDSClusterContainer.awsRDSPlatform = this.awsRDSPlatform;
            return awsRDSClusterContainer;
        }
    }
}
