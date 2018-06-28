package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.fateengine.FateManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryPlatform implements Platform {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPlatform.class);
    private static final String CLOUDFOUNDRY_RUNNING_STATE = "RUNNING";
    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;
    @Autowired
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    @Autowired
    private CloudFoundryClient cloudFoundryClient;
    @Autowired
    private ContainerManager containerManager;
    @Autowired
    private FateManager fateManager;
    @Autowired(required = false)
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    @Autowired
    CloudFoundryPlatform () {
    }

    CloudFoundryPlatform (CloudFoundryPlatformInfo cloudFoundryPlatformInfo, CloudFoundryOperations cloudFoundryOperations, ContainerManager containerManager) {
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.containerManager = containerManager;
    }

    public CloudFoundryPlatformInfo getCloudFoundryPlatformInfo () {
        cloudFoundryPlatformInfo.fetchInfo();
        return cloudFoundryPlatformInfo;
    }

    public void degrade (Container container) {
        if (!(container instanceof CloudFoundryContainer)) {
            throw new ChaosException("Expected to be passed a Cloud Foundry container");
        }
        log.info("Attempting to degrade performance on {}", container);
        // TODO : Implement container degradation
    }

    @Override
    public List<Container> getRoster () {
        List<Container> containers = new ArrayList<>();
        cloudFoundryOperations.applications().list().toIterable().forEach(app -> {
            Integer instances = app.getInstances();
            for (Integer i = 0; i < instances; i++) {
                if (isChaosEngine(app.getName(), i)) {
                    log.debug("Skipping what appears to be me.");
                    continue;
                }
                CloudFoundryContainer c = CloudFoundryContainer.builder()
                                                               .applicationId(app.getId())
                                                               .name(app.getName())
                                                               .instance(i)
                                                               .platform(this)
                                                               .fateManager(fateManager)
                                                               .build();
                Container persistentContainer = containerManager.getOrCreatePersistentContainer(c);
                containers.add(persistentContainer);
                if (persistentContainer == c) {
                    log.info("Added container {}", persistentContainer);
                } else {
                    log.debug("Existing container found: {}", persistentContainer);
                }
            }
        });
        containerManager.removeOldContainers(CloudFoundryContainer.class, containers);
        return containers;
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

    public void restartInstance (RestartApplicationInstanceRequest restartApplicationInstanceRequest) {
        cloudFoundryOperations.applications().restartInstance(restartApplicationInstanceRequest).block();
    }

    public ContainerHealth checkHealth (String applicationId, AttackType attackType, Integer instanceId) {
        if (attackType == AttackType.STATE) {
            Map<String, ApplicationInstanceInfo> applicationInstanceResponse;
            applicationInstanceResponse = cloudFoundryClient.applicationsV2()
                                                            .instances(ApplicationInstancesRequest.builder()
                                                                                                  .applicationId(applicationId)
                                                                                                  .build())
                                                            .block()
                                                            .getInstances();
            String status;
            try {
                status = applicationInstanceResponse.get(instanceId.toString()).getState();
            } catch (NullPointerException e) {
                return ContainerHealth.DOES_NOT_EXIST;
            }
            return (status.equals(CLOUDFOUNDRY_RUNNING_STATE) ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK);

        } else {
            // TODO: Implement Health Checks for other attack types.
            return ContainerHealth.NORMAL;
        }
    }

    private boolean isChaosEngine (String applicationName, Integer instanceId) {
        return cloudFoundrySelfAwareness != null && (cloudFoundrySelfAwareness.isMe(applicationName, instanceId) || cloudFoundrySelfAwareness
                .isFriendly(applicationName));
    }
}
