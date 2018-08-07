package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.constants.CloudFoundryConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryContainer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

@Component
@ConditionalOnProperty({ "cf_organization" })
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

    public void restageApplication (RestageApplicationRequest restageApplicationRequest) {
        cloudFoundryOperations.applications().restage(restageApplicationRequest).block();
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
        return (status.equals(CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE) ? ContainerHealth.NORMAL : ContainerHealth.UNDER_ATTACK);
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
                                  if (!isChaosEngine(app.getName())) {
                                      for (Integer i = 0; i < instances; i++) {
                                          createContainerFromApp(containers, app, i);
                                      }
                                  } else {
                                      log.debug("Skipping what appears to be me.");
                                  }
                              });
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

    public void restartInstance (RestartApplicationInstanceRequest restartApplicationInstanceRequest) {
        cloudFoundryOperations.applications().restartInstance(restartApplicationInstanceRequest).block();
    }
}
