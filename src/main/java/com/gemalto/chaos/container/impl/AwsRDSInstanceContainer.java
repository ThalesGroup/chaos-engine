package com.gemalto.chaos.container.impl;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;
import com.gemalto.chaos.constants.AwsRDSConstants;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.gemalto.chaos.constants.AwsRDSConstants.AWS_RDS_INSTANCE_DATADOG_IDENTIFIER;
import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;
import static net.logstash.logback.argument.StructuredArguments.*;

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
    public void restartInstance (Experiment experiment) {
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifier));
        awsRDSPlatform.restartInstance(dbInstanceIdentifier);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withKey(AWS_RDS_INSTANCE_DATADOG_IDENTIFIER).withValue(dbInstanceIdentifier);
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
        awsRDSPlatform.setVpcSecurityGroupIds(dbInstanceIdentifier, awsRDSPlatform.getChaosSecurityGroup());
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
            awsRDSInstanceContainer.log.info("Created new AWS RDS Instance Container object", this.dataDogTags.entrySet()
                                                                                                              .stream()
                                                                                                              .map(entry -> kv(entry
                                                                                                                      .getKey(), entry
                                                                                                                      .getValue()))
                                                                                                              .toArray());

            return awsRDSInstanceContainer;
        }
    }
}
