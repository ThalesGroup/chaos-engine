package com.thales.chaos.container.impl;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;
import com.thales.chaos.constants.AwsRDSConstants;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.AwsContainer;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.NetworkExperiment;
import com.thales.chaos.experiment.annotations.StateExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.AwsRDSPlatform;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.thales.chaos.constants.AwsRDSConstants.AWS_RDS_INSTANCE_DATADOG_IDENTIFIER;
import static net.logstash.logback.argument.StructuredArguments.v;
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

    @Override
    public String getAggregationIdentifier () {
        return dbInstanceIdentifier;
    }

    @StateExperiment
    public void restartInstance (Experiment experiment) {
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier));
        awsRDSPlatform.restartInstance(dbInstanceIdentifier);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER).withValue(dbInstanceIdentifier);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(dbInstanceIdentifier);
    }

    @NetworkExperiment
    public void removeSecurityGroups (Experiment experiment) {
        Collection<String> existingSecurityGroups = awsRDSPlatform.getVpcSecurityGroupIds(dbInstanceIdentifier);
        log.info("Existing security groups for {} are {}", value(getDataDogIdentifier().getKey(), getDataDogIdentifier().getValue()), value("securityGroups", existingSecurityGroups));
        experiment.setSelfHealingMethod(() -> {
            awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, existingSecurityGroups);
            return null;
        });
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.checkVpcSecurityGroupIds(dbInstanceIdentifier, existingSecurityGroups));
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, awsRDSPlatform.getChaosSecurityGroup(dbInstanceIdentifier));
    }

    @StateExperiment
    public void startSnapshot (Experiment experiment) {
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.isInstanceSnapshotRunning(dbInstanceIdentifier) ? ContainerHealth.RUNNING_EXPERIMENT : ContainerHealth.NORMAL);
        final DBSnapshot dbSnapshot = awsRDSPlatform.snapshotDBInstance(dbInstanceIdentifier);
        experiment.setSelfHealingMethod(() -> {
            try {
                awsRDSPlatform.deleteInstanceSnapshot(dbSnapshot);
            } catch (DBSnapshotNotFoundException e) {
                log.warn("Attempted to delete snapshot, but it was already deleted", v(DataDogConstants.RDS_INSTANCE_SNAPSHOT, dbSnapshot), e);
            }
            return null;
        });
        experiment.setFinalizeMethod(experiment.getSelfHealingMethod());
    }

    public static AwsRDSInstanceContainerBuilder builder () {
        return AwsRDSInstanceContainerBuilder.anAwsRDSInstanceContainer();
    }

    public static final class AwsRDSInstanceContainerBuilder {
        private String dbInstanceIdentifier;
        private String engine;
        private String availabilityZone;
        private AwsRDSPlatform awsRDSPlatform;
        private final Map<String, String> dataDogTags = new HashMap<>();

        private AwsRDSInstanceContainerBuilder () {
        }

        static AwsRDSInstanceContainerBuilder anAwsRDSInstanceContainer () {
            return new AwsRDSInstanceContainerBuilder();
        }

        public AwsRDSInstanceContainerBuilder withDbInstanceIdentifier (String dbInstanceIdentifier) {
            this.dbInstanceIdentifier = dbInstanceIdentifier;
            return withDataDogTag(AwsRDSConstants.AWS_RDS_INSTANCE_DATADOG_IDENTIFIER, dbInstanceIdentifier);
        }

        public AwsRDSInstanceContainerBuilder withDataDogTag (String key, String value) {
            dataDogTags.put(key, value);
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

        public AwsRDSInstanceContainerBuilder withDbiResourceId (String dbiResourceId) {
            return withDataDogTag(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, dbiResourceId);
        }

        public AwsRDSInstanceContainer build () {
            AwsRDSInstanceContainer awsRDSInstanceContainer = new AwsRDSInstanceContainer();
            awsRDSInstanceContainer.dbInstanceIdentifier = this.dbInstanceIdentifier;
            awsRDSInstanceContainer.engine = this.engine;
            awsRDSInstanceContainer.awsRDSPlatform = this.awsRDSPlatform;
            awsRDSInstanceContainer.availabilityZone = this.availabilityZone;
            awsRDSInstanceContainer.dataDogTags.putAll(this.dataDogTags);
            try {
                awsRDSInstanceContainer.setMappedDiagnosticContext();
                awsRDSInstanceContainer.log.info("Created new AWS RDS Instance Container object");
            } finally {
                awsRDSInstanceContainer.clearMappedDiagnosticContext();
            }
            return awsRDSInstanceContainer;
        }
    }
}
