package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.CloudFoundryConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.ssh.SshExperiment;
import com.gemalto.chaos.ssh.impl.CloudFoundrySshManager;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_CONTAINER_KEY;
import static net.logstash.logback.argument.StructuredArguments.v;

@Component
@ConditionalOnProperty({ "cf.organization" })
@ConfigurationProperties("cf")
public class CloudFoundryContainerPlatform extends CloudFoundryPlatform {
    private CloudFoundryOperations cloudFoundryOperations;
    private ContainerManager containerManager;
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    private CloudFoundryClient cloudFoundryClient;

    @Autowired
    public CloudFoundryContainerPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryPlatformInfo cloudFoundryPlatformInfo, CloudFoundryClient cloudFoundryClient, ContainerManager containerManager) {
        super(cloudFoundryOperations, cloudFoundryPlatformInfo);
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
        this.cloudFoundryClient = cloudFoundryClient;
        this.containerManager = containerManager;
    }

    public static CloudFoundryContainerPlatformBuilder builder () {
        return CloudFoundryContainerPlatformBuilder.builder();
    }

    public ContainerHealth checkHealth (String applicationId, Integer instanceId) {
        Map<String, ApplicationInstanceInfo> applicationInstanceResponse;
        try {
            applicationInstanceResponse = cloudFoundryClient.applicationsV2()
                                                            .instances(ApplicationInstancesRequest.builder()
                                                                                                  .applicationId(applicationId)
                                                                                                  .build())
                                                            .block()
                                                            .getInstances();
        } catch (ClientV2Exception e) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        String status;
        try {
            status = applicationInstanceResponse.get(instanceId.toString()).getState();
        } catch (NullPointerException e) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return (status.equals(CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
    }

    @Override
    public List<Container> generateRoster () {
        return cloudFoundryOperations.applications()
                                     .list()
                                     .filter(app -> app.getRequestedState().equals(CLOUDFOUNDRY_APPLICATION_STARTED))
                                     .toStream(Integer.MAX_VALUE)
                                     .filter(applicationSummary -> {
                                         if (isChaosEngine(applicationSummary.getName())) {
                                             log.debug("Ignored what appears to be me: {}", v("ApplicationSummary", applicationSummary));
                                             return false;
                                         }
                                         return true;
                                     })
                                     .map(this::createContainersFromApplicationSummary)
                                     .flatMap(Collection::stream)
                                     .collect(Collectors.toList());

    }

    Collection<CloudFoundryContainer> createContainersFromApplicationSummary (ApplicationSummary applicationSummary) {
        Integer instances = applicationSummary.getInstances();
        return IntStream.range(0, instances)
                        .mapToObj(i -> createSingleContainerFromApplicationSummary(applicationSummary, i))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    CloudFoundryContainer createSingleContainerFromApplicationSummary (ApplicationSummary applicationSummary, Integer index) {
        CloudFoundryContainer container = containerManager.getMatchingContainer(CloudFoundryContainer.class, applicationSummary
                .getName() + "-" + index);
        if (container == null) {
            container = CloudFoundryContainer.builder()
                                             .applicationId(applicationSummary.getId())
                                             .name(applicationSummary.getName())
                                             .instance(index)
                                             .platform(this)
                                             .build();
            log.debug("Created Cloud Foundry Container {} from {}", v(DATADOG_CONTAINER_KEY, container), v("ApplicationSummary", applicationSummary));
            containerManager.offer(container);
        } else {
            log.debug("Found existing Cloud Foundry Container {}", v(DATADOG_CONTAINER_KEY, container));
        }
        return container;
    }

    public void restartInstance (RestartApplicationInstanceRequest restartApplicationInstanceRequest) {
        cloudFoundryOperations.applications().restartInstance(restartApplicationInstanceRequest).block();
    }

    public void sshExperiment (SshExperiment sshExperiment, CloudFoundryContainer container) {
        try {
            CloudFoundrySshManager ssh = new CloudFoundrySshManager(getCloudFoundryPlatformInfo());
            if (ssh.connect(container)) {
                if (container.getDetectedCapabilities() != null) {
                    sshExperiment.setShellSessionCapabilities(container.getDetectedCapabilities());
                }
                sshExperiment.runExperiment(ssh);
                if (container.getDetectedCapabilities() == null || container.getDetectedCapabilities() != sshExperiment.getShellSessionCapabilities()) {
                    container.setDetectedCapabilities(sshExperiment.getShellSessionCapabilities());
                }
                ssh.disconnect();
            }
        } catch (IOException e) {
           throw new ChaosException(e);
        }
    }

    public static final class CloudFoundryContainerPlatformBuilder {
        private CloudFoundryOperations cloudFoundryOperations;
        private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
        private CloudFoundryClient cloudFoundryClient;
        private ContainerManager containerManager;
        private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

        private CloudFoundryContainerPlatformBuilder () {
        }

        private static CloudFoundryContainerPlatformBuilder builder () {
            return new CloudFoundryContainerPlatformBuilder();
        }

        CloudFoundryContainerPlatformBuilder withCloudFoundryOperations (CloudFoundryOperations cloudFoundryOperations) {
            this.cloudFoundryOperations = cloudFoundryOperations;
            return this;
        }

        CloudFoundryContainerPlatformBuilder withCloudFoundryPlatformInfo (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
            this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
            return this;
        }

        CloudFoundryContainerPlatformBuilder withCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
            this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
            return this;
        }

        CloudFoundryContainerPlatformBuilder withCloudFoundryClient (CloudFoundryClient cloudFoundryClient) {
            this.cloudFoundryClient = cloudFoundryClient;
            return this;
        }

        CloudFoundryContainerPlatformBuilder withContainerManager (ContainerManager containerManager) {
            this.containerManager = containerManager;
            return this;
        }

        public CloudFoundryContainerPlatform build () {
            CloudFoundryContainerPlatform cloudFoundryContainerPlatform = new CloudFoundryContainerPlatform(cloudFoundryOperations, cloudFoundryPlatformInfo, cloudFoundryClient, containerManager);
            cloudFoundryContainerPlatform.setCloudFoundrySelfAwareness(this.cloudFoundrySelfAwareness);
            return cloudFoundryContainerPlatform;
        }
    }
}
