package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;

import java.util.Collection;

import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;
import static net.logstash.logback.argument.StructuredArguments.value;

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
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier);
    }

    @Override
    public String getSimpleName () {
        return getDbInstanceIdentifier();
    }

    @StateExperiment
    public void restartInstance (Experiment attack) {
        attack.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier));
        awsRDSPlatform.restartInstance(dbInstanceIdentifier);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withKey("dbinstanceidentifier").withValue(dbInstanceIdentifier);
    }

    @NetworkExperiment
    public void removeSecurityGroups (Experiment attack) {
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
            DataDogIdentifier dataDogIdentifier = awsRDSInstanceContainer.getDataDogIdentifier();
            awsRDSInstanceContainer.log.info("Created AWS RDS Instance Container {}", value(dataDogIdentifier.getKey(), dataDogIdentifier
                    .getValue()));
            return awsRDSInstanceContainer;
        }
    }
}
