/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.container.impl;

import com.amazonaws.services.rds.model.DBClusterNotFoundException;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.constants.AwsConstants;
import com.thales.chaos.constants.AwsRDSConstants;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.AwsContainer;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.AwsChaosErrorCode;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.AwsRDSPlatform;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.thales.chaos.constants.AwsRDSConstants.AWS_RDS_CLUSTER_DATADOG_IDENTIFIER;
import static net.logstash.logback.argument.StructuredArguments.v;
import static net.logstash.logback.argument.StructuredArguments.value;

public class AwsRDSClusterContainer extends AwsContainer {
    @Identifier(order = 0)
    private String dbClusterIdentifier;
    @Identifier(order = 1)
    private String engine;
    private AwsRDSPlatform awsRDSPlatform;

    public static AwsRDSClusterContainerBuilder builder () {
        return AwsRDSClusterContainerBuilder.anAwsRDSClusterContainer();
    }

    public String getEngine () {
        return engine;
    }

    @Override
    public Platform getPlatform () {
        return awsRDSPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return awsRDSPlatform.getInstanceStatus(getMembers().toArray(new String[0]));
    }

    @Override
    public String getSimpleName () {
        return getDbClusterIdentifier();
    }

    @Override
    public String getAggregationIdentifier () {
        return dbClusterIdentifier;
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey(AWS_RDS_CLUSTER_DATADOG_IDENTIFIER).withValue(dbClusterIdentifier);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(dbClusterIdentifier);
    }

    public String getDbClusterIdentifier () {
        return dbClusterIdentifier;
    }

    @JsonIgnore
    public Set<String> getMembers () {
        return awsRDSPlatform.getClusterInstances(dbClusterIdentifier);
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void restartInstances (Experiment experiment) {
        final String[] dbInstanceIdentifiers = getSomeMembers().toArray(new String[0]);
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifiers));
        awsRDSPlatform.restartInstance(dbInstanceIdentifiers);
    }

    /**
     * @return A randomly generated subset of getMembers. This will always return at least 1, and at most N-1 entries.
     */
    Set<String> getSomeMembers () {
        Set<String> someMembers = getSomeMembersInner();
        log.info("Experiment using cluster members {}", value("experimentMembers", someMembers));
        return someMembers;
    }

    /**
     * @return A randomly generated subset of getMembers. This will always return at least 1, and at most N-1 entries.
     */
    Set<String> getSomeMembersInner () {
        Set<String> returnSet;
        // Make members a List instead of Set so it can be sorted.
        List<String> members = new ArrayList<>(getMembers());
        Collections.shuffle(members, ThreadLocalRandom.current());
        // If there are 0 or 1 members in a cluster, we cannot choose a subset.
        if (members.size() <= 1) {
            throw new ChaosException(AwsChaosErrorCode.SINGLE_INSTANCE_CLUSTER);
        } else if (members.size() == 2) {
            // If there are exactly 2 members, the only valid subset is of size 1. Since the set is shuffled,
            // we can just return index 0 (as a set).
            String member = members.get(0);
            return Collections.singleton(member);
        }
        returnSet = new HashSet<>();
        // Offsetting -1/+1 to ensure that a minimum of 1 item is set. nextInt is exclusive on upper bound,
        // so the full size is not an option.
        int upperLimit = ThreadLocalRandom.current().nextInt(members.size() - 1) + 1;
        for (int i = 0; i < upperLimit; i++) {
            returnSet.add(members.get(i));
        }
        return returnSet;
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void startSnapshot (Experiment experiment) {
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.isClusterSnapshotRunning(dbClusterIdentifier) ? ContainerHealth.RUNNING_EXPERIMENT : ContainerHealth.NORMAL);
        final DBClusterSnapshot dbClusterSnapshot = awsRDSPlatform.snapshotDBCluster(dbClusterIdentifier);
        experiment.setSelfHealingMethod(() -> {
            try {
                awsRDSPlatform.deleteClusterSnapshot(dbClusterSnapshot);
            } catch (DBClusterNotFoundException e) {
                log.warn("Tried to clean up cluster snapshot, but it was already deleted", v(DataDogConstants.RDS_CLUSTER_SNAPSHOT, dbClusterSnapshot), e);
            }
        });
        experiment.setFinalizeMethod(experiment.getSelfHealingMethod());
        // On finalize clean up the snapshot.
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void initiateFailover (Experiment experiment) {
        final String[] members = getMembers().toArray(new String[0]);
        experiment.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(members));
        awsRDSPlatform.failoverCluster(dbClusterIdentifier);
    }

    public static final class AwsRDSClusterContainerBuilder {
        private final Map<String, String> dataDogTags = new HashMap<>();
        private String dbClusterIdentifier;
        private String engine;
        private String availabilityZone;
        private AwsRDSPlatform awsRDSPlatform;

        private AwsRDSClusterContainerBuilder () {
        }

        static AwsRDSClusterContainerBuilder anAwsRDSClusterContainer () {
            return new AwsRDSClusterContainerBuilder();
        }

        public AwsRDSClusterContainerBuilder withAvailabilityZone (String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public AwsRDSClusterContainerBuilder withDbClusterIdentifier (String dbClusterIdentifier) {
            this.dbClusterIdentifier = dbClusterIdentifier;
            return withDataDogTag(AwsRDSConstants.AWS_RDS_CLUSTER_DATADOG_IDENTIFIER, dbClusterIdentifier);
        }

        public AwsRDSClusterContainerBuilder withEngine (String engine) {
            this.engine = engine;
            return this;
        }

        public AwsRDSClusterContainerBuilder withAwsRDSPlatform (AwsRDSPlatform awsRDSPlatform) {
            this.awsRDSPlatform = awsRDSPlatform;
            return this;
        }

        public AwsRDSClusterContainerBuilder withDataDogTag (String key, String value) {
            this.dataDogTags.put(key, value);
            return this;
        }

        public AwsRDSClusterContainerBuilder withDBClusterResourceId (String dbClusterResourceId) {
            return withDataDogTag(DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY, dbClusterResourceId);
        }

        public AwsRDSClusterContainer build () {
            AwsRDSClusterContainer awsRDSClusterContainer = new AwsRDSClusterContainer();
            awsRDSClusterContainer.engine = this.engine;
            awsRDSClusterContainer.dbClusterIdentifier = this.dbClusterIdentifier;
            awsRDSClusterContainer.awsRDSPlatform = this.awsRDSPlatform;
            awsRDSClusterContainer.availabilityZone = this.availabilityZone != null ? this.availabilityZone : AwsConstants.NO_AZ_INFORMATION;
            awsRDSClusterContainer.dataDogTags.putAll(this.dataDogTags);
            try {
                awsRDSClusterContainer.setMappedDiagnosticContext();
                awsRDSClusterContainer.log.info("Created new AWS RDS Cluster Container object");
            } finally {
                awsRDSClusterContainer.clearMappedDiagnosticContext();
            }
            return awsRDSClusterContainer;
        }
    }
}
