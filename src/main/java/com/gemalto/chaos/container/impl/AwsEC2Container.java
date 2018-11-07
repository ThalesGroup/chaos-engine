package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.AwsContainer;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gemalto.chaos.constants.AwsEC2Constants.AWS_EC2_HARD_REBOOT_TIMER_MINUTES;
import static com.gemalto.chaos.notification.datadog.DataDogIdentifier.dataDogIdentifier;

public class AwsEC2Container extends AwsContainer {
    private String instanceId;
    private String keyName;
    private String name;
    private String groupIdentifier;
    private transient AwsEC2Platform awsEC2Platform;
    private final transient Callable<Void> startContainerMethod = () -> {
        awsEC2Platform.startInstance(instanceId);
        return null;
    };
    private final transient Callable<ContainerHealth> checkContainerStartedMethod = () -> awsEC2Platform.checkHealth(instanceId);
    private AwsEC2Container () {
        super();
    }

    public static AwsEC2ContainerBuilder builder () {
        return AwsEC2ContainerBuilder.anAwsEC2Container();
    }

    public String getInstanceId () {
        return instanceId;
    }

    public String getGroupIdentifier () {
        return groupIdentifier;
    }

    @Override
    public Platform getPlatform () {
        return awsEC2Platform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return awsEC2Platform.checkHealth(instanceId);
    }

    @Override
    public String getSimpleName () {
        return String.format("%s (%s) [%s]", name, keyName, instanceId);
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return dataDogIdentifier().withValue(instanceId);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(instanceId);
    }

    @StateExperiment
    public void stopContainer (Experiment experiment) {
        awsEC2Platform.stopInstance(instanceId);
        experiment.setSelfHealingMethod(startContainerMethod);
        experiment.setCheckContainerHealth(checkContainerStartedMethod);
    }

    @StateExperiment
    public void restartContainer (Experiment experiment) {
        final Instant hardRebootTimer = Instant.now().plus(Duration.ofMinutes(AWS_EC2_HARD_REBOOT_TIMER_MINUTES));
        awsEC2Platform.restartInstance(instanceId);
        experiment.setSelfHealingMethod(startContainerMethod);
        experiment.setCheckContainerHealth(() -> hardRebootTimer.isBefore(Instant.now()) ? awsEC2Platform.checkHealth(instanceId) : ContainerHealth.RUNNING_EXPERIMENT);
        // If Ctrl+Alt+Del is disabled in the AMI, then it takes 4 minutes for EC2 to initiate a hard reboot.
    }

    @NetworkExperiment
    public void removeSecurityGroups (Experiment experiment) {
        List<String> originalSecurityGroupIds = awsEC2Platform.getSecurityGroupIds(instanceId);
        awsEC2Platform.setSecurityGroupIds(instanceId, Collections.singletonList(awsEC2Platform.getChaosSecurityGroupId()));
        experiment.setCheckContainerHealth(() -> awsEC2Platform.verifySecurityGroupIds(instanceId, originalSecurityGroupIds));
        experiment.setSelfHealingMethod(() -> {
            awsEC2Platform.setSecurityGroupIds(instanceId, originalSecurityGroupIds);
            return null;
        });
    }

    public static final class AwsEC2ContainerBuilder {
        private String instanceId;
        private String keyName;
        private String name;
        private AwsEC2Platform awsEC2Platform;
        private String availabilityZone;
        private String groupIdentifier;

        private AwsEC2ContainerBuilder () {
        }

        static AwsEC2ContainerBuilder anAwsEC2Container () {
            return new AwsEC2ContainerBuilder();
        }

        public AwsEC2ContainerBuilder instanceId (String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public AwsEC2ContainerBuilder keyName (String keyName) {
            this.keyName = keyName;
            return this;
        }

        public AwsEC2ContainerBuilder name (String name) {
            this.name = name;
            return this;
        }

        public AwsEC2ContainerBuilder awsEC2Platform (AwsEC2Platform awsEC2Platform) {
            this.awsEC2Platform = awsEC2Platform;
            return this;
        }

        public AwsEC2ContainerBuilder availabilityZone (String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public AwsEC2ContainerBuilder groupIdentifier (String groupIdentifier) {
            this.groupIdentifier = groupIdentifier;
            return this;
        }


        public AwsEC2Container build () {
            AwsEC2Container awsEC2Container = new AwsEC2Container();
            awsEC2Container.awsEC2Platform = this.awsEC2Platform;
            awsEC2Container.instanceId = this.instanceId;
            awsEC2Container.keyName = this.keyName;
            awsEC2Container.name = this.name;
            awsEC2Container.availabilityZone = this.availabilityZone;
            awsEC2Container.groupIdentifier = this.groupIdentifier;
            return awsEC2Container;
        }
    }
}
