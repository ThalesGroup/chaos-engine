package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsRDSPlatform;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.gemalto.chaos.constants.AwsConstants.NO_AZ_INFORMATION;

public class AwsRDSClusterContainer extends AwsContainer {
    private String dbClusterIdentifier;
    private String engine;
    private transient AwsRDSPlatform awsRDSPlatform;

    public static AwsRDSClusterContainerBuilder builder () {
        return AwsRDSClusterContainerBuilder.anAwsRDSClusterContainer();
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
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withKey("dbClusterIdentifier").withValue(dbClusterIdentifier);
    }

    public String getDbClusterIdentifier () {
        return dbClusterIdentifier;
    }

    public Set<String> getMembers () {
        return awsRDSPlatform.getClusterInstances(dbClusterIdentifier);
    }

    @StateExperiment
    public void restartInstances (Experiment attack) {
        final String[] dbInstanceIdentifiers = getSomeMembers().toArray(new String[0]);
        attack.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(dbInstanceIdentifiers));
        awsRDSPlatform.restartInstance(dbInstanceIdentifiers);
    }

    /**
     * @return A randomly generated subset of getMembers. This will always return at least 1, and at most N-1 entries.
     */
    Set<String> getSomeMembers () {
        Set<String> returnSet;
        // Make members a List instead of Set so it can be sorted.
        List<String> members = new ArrayList<>(getMembers());
        Collections.shuffle(members, ThreadLocalRandom.current());
        // If there are 0 or 1 members in a cluster, we cannot choose a subset.
        if (members.size() <= 1) {
            throw new ChaosException("Cluster contains less than 2 instances. Cannot operate on a subset.");
        } else if (members.size() == 2) {
            // If there are exactly 2 members, the only valid subset is of size 1. Since the set is shuffled,
            // we can just return index 0 (as a set).
            return Collections.singleton(members.get(0));
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

    @StateExperiment
    public void initiateFailover (Experiment attack) {
        final String[] members = getMembers().toArray(new String[0]);
        attack.setCheckContainerHealth(() -> awsRDSPlatform.getInstanceStatus(members));
        awsRDSPlatform.failoverCluster(dbClusterIdentifier);
    }

    public static final class AwsRDSClusterContainerBuilder {
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
            awsRDSClusterContainer.availabilityZone = this.availabilityZone != null ? this.availabilityZone : NO_AZ_INFORMATION;
            return awsRDSClusterContainer;
        }
    }
}
