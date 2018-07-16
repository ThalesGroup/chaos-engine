package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.constants.CloudFoundryConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import com.gemalto.chaos.ssh.SshAttack;
import com.gemalto.chaos.ssh.impl.CloudFoundrySshManager;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryPlatform extends Platform {

    private CloudFoundryOperations cloudFoundryOperations;

    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    private CloudFoundryClient cloudFoundryClient;
    private ContainerManager containerManager;

    @Autowired
    public CloudFoundryPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryPlatformInfo cloudFoundryPlatformInfo, CloudFoundryClient cloudFoundryClient, ContainerManager containerManager) {
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
        this.cloudFoundryClient = cloudFoundryClient;
        this.containerManager = containerManager;
    }


    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    private CloudFoundryPlatform () {
    }

    @Autowired(required = false)
    private void setCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
        this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
    }



    public static CloudFoundryPlatformBuilder builder () {
        return CloudFoundryPlatformBuilder.builder();
    }

    public CloudFoundryPlatformInfo getCloudFoundryPlatformInfo () {
        cloudFoundryPlatformInfo.fetchInfo();
        return cloudFoundryPlatformInfo;
    }

    @Override
    public List<Container> generateRoster () {
        List<Container> containers = new ArrayList<>();
        cloudFoundryOperations.applications()
                              .list()
                              .filter(app -> app.getRequestedState().equals(CLOUDFOUNDRY_APPLICATION_STARTED))
                              .toIterable()
                              .forEach(app -> {
                                  Integer instances = app.getInstances();
                                  for (Integer i = 0; i < instances; i++) {
                                      if (isChaosEngine(app.getName(), i)) {
                                          log.debug("Skipping what appears to be me.");
                                          continue;
                                      }
                                      createContainerFromApp(containers, app, i);
                                  }
                              });
        containerManager.removeOldContainers(CloudFoundryContainer.class, containers);
        return containers;
    }

    private void createContainerFromApp (List<Container> containers, ApplicationSummary app, Integer i) {
        CloudFoundryContainer c = CloudFoundryContainer.builder()
                                                       .applicationId(app.getId())
                                                       .name(app.getName())
                                                       .instance(i)
                                                       .platform(this)
                                                       .build();
        Container persistentContainer = containerManager.getOrCreatePersistentContainer(c);
        containers.add(persistentContainer);
        if (persistentContainer == c) {
            log.info("Added container {}", persistentContainer);
        } else {
            log.debug("Existing container found: {}", persistentContainer);
        }
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            cloudFoundryOperations.applications().list();
            return ApiStatus.OK;
        } catch (RuntimeException e) {
            log.error("Failed to load application list", e);
            return ApiStatus.ERROR;
        }
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.PAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        Flux<ApplicationSummary> runningInstances = cloudFoundryOperations.applications()
                                                                          .list()
                                                                          .filter(a -> a.getRequestedState()
                                                                                        .equals(CLOUDFOUNDRY_APPLICATION_STARTED));
        if (runningInstances.filter(a -> a.getInstances() > 0)
                            .filter(a -> a.getRunningInstances() == 0)
                            .hasElements()
                            .block()) {
            return PlatformHealth.FAILED;
        } else if (runningInstances.filter(a -> a.getInstances() > 0)
                                   .filter(a -> a.getRunningInstances() < a.getInstances())
                                   .hasElements()
                                   .block()) {
            return PlatformHealth.DEGRADED;
        }
        return PlatformHealth.OK;
    }

    private boolean isChaosEngine (String applicationName, Integer instanceId) {
        return cloudFoundrySelfAwareness != null && (cloudFoundrySelfAwareness.isMe(applicationName, instanceId) || cloudFoundrySelfAwareness
                .isFriendly(applicationName));
    }

    public void restartInstance (RestartApplicationInstanceRequest restartApplicationInstanceRequest) {
        cloudFoundryOperations.applications().restartInstance(restartApplicationInstanceRequest).block();
    }

    public void sshAttack (SshAttack attack, CloudFoundryContainer container) {
        CloudFoundrySshManager ssh = new CloudFoundrySshManager(getCloudFoundryPlatformInfo());
        if (ssh.connect(container)) {
            attack.attack(ssh);
            ssh.disconnect();
        }
    }

    public ContainerHealth checkHealth (String applicationId, AttackType attackType, Integer instanceId) {
        if (attackType == AttackType.STATE) {
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
            return (status.equals(CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE) ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK);
        } else {
            // TODO: Implement Health Checks for other attack types.
            return ContainerHealth.NORMAL;
        }
    }

    public void restageApplication (RestageApplicationRequest restageApplicationRequest) {
        cloudFoundryOperations.applications().restage(restageApplicationRequest).block();
    }

    public static final class CloudFoundryPlatformBuilder {
        private CloudFoundryOperations cloudFoundryOperations;
        private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
        private CloudFoundryClient cloudFoundryClient;
        private ContainerManager containerManager;
        private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

        private CloudFoundryPlatformBuilder () {
        }

        private static CloudFoundryPlatformBuilder builder () {
            return new CloudFoundryPlatformBuilder();
        }

        CloudFoundryPlatformBuilder withCloudFoundryOperations (CloudFoundryOperations cloudFoundryOperations) {
            this.cloudFoundryOperations = cloudFoundryOperations;
            return this;
        }

        CloudFoundryPlatformBuilder withCloudFoundryPlatformInfo (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
            this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
            return this;
        }

        CloudFoundryPlatformBuilder withCloudFoundryClient (CloudFoundryClient cloudFoundryClient) {
            this.cloudFoundryClient = cloudFoundryClient;
            return this;
        }

        CloudFoundryPlatformBuilder withContainerManager (ContainerManager containerManager) {
            this.containerManager = containerManager;
            return this;
        }

        CloudFoundryPlatformBuilder withCloudFoundrySelfAwareness (CloudFoundrySelfAwareness cloudFoundrySelfAwareness) {
            this.cloudFoundrySelfAwareness = cloudFoundrySelfAwareness;
            return this;
        }

        public CloudFoundryPlatform build () {
            CloudFoundryPlatform cloudFoundryPlatform = new CloudFoundryPlatform();
            cloudFoundryPlatform.cloudFoundrySelfAwareness = this.cloudFoundrySelfAwareness;
            cloudFoundryPlatform.cloudFoundryClient = this.cloudFoundryClient;
            cloudFoundryPlatform.cloudFoundryOperations = this.cloudFoundryOperations;
            cloudFoundryPlatform.containerManager = this.containerManager;
            cloudFoundryPlatform.cloudFoundryPlatformInfo = this.cloudFoundryPlatformInfo;
            return cloudFoundryPlatform;
        }
    }
}
