package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.constants.CloudFoundryConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryApplication;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryApplicationPlatform extends CloudFoundryPlatform {
    private CloudFoundryOperations cloudFoundryOperations;
    private CloudFoundryClient cloudFoundryClient;

    @Autowired
    public CloudFoundryApplicationPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient, CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryOperations, cloudFoundryPlatformInfo);
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.cloudFoundryClient = cloudFoundryClient;
    }

    public ContainerHealth checkPlatformHealth () {
        Iterable<ApplicationSummary> apps = cloudFoundryOperations.applications()
                                                                  .list()
                                                                  .filter(app -> app.getRequestedState()
                                                                                    .equals(CLOUDFOUNDRY_APPLICATION_STARTED))
                                                                  .filter(app -> app.getInstances() > 0)
                                                                  .toIterable();
        for (ApplicationSummary app : apps) {
            ContainerHealth appHealth = checkApplicationHealth(app.getName(), app.getId());
            if (ContainerHealth.NORMAL != appHealth) {
                return appHealth;
            }
        }
        return ContainerHealth.NORMAL;
    }

    private ContainerHealth checkApplicationHealth (String applicationName, String applicationID) {
        Map<String, ApplicationInstanceInfo> applicationInstanceResponse;
        try {
            applicationInstanceResponse = cloudFoundryClient.applicationsV2()
                                                            .instances(ApplicationInstancesRequest.builder()
                                                                                                  .applicationId(applicationID)
                                                                                                  .build())
                                                            .block()
                                                            .getInstances();
        } catch (ClientV2Exception e) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        String status;
        try {
            status = CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE;
            for (Map.Entry<String, ApplicationInstanceInfo> appInfo : applicationInstanceResponse.entrySet()) {
                log.debug("Checking container {} {}", applicationName, appInfo.getValue().getState());
                if (!CloudFoundryConstants.CLOUDFOUNDRY_RUNNING_STATE.equals(appInfo.getValue().getState())) {
                    status = appInfo.getValue().getState();
                    break;
                }
            }
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
                                  if (instances <= 0) {
                                      log.debug("Skipping {} which has {} container instances.", app.getName(), instances);
                                  } else {
                                      if (!isChaosEngine(app.getName())) {
                                          createApplication(containers, app, instances);
                                      } else {
                                          log.debug("Skipping {} what appears to be me.", app.getName());
                                      }
                                  }
                              });
        return containers;
    }

    private void createApplication (List<Container> containers, ApplicationSummary app, Integer containerInstances) {
        CloudFoundryApplication application = CloudFoundryApplication.builder()
                                                                     .platform(this)
                                                                     .name(app.getName()).applicationID(app.getId())
                                                                     .containerInstances(containerInstances)
                                                                     .build();
        containers.add(application);
        log.info("Added application {}", application);
    }

    public void rescaleApplication (String applicationName, int instances) {
        ScaleApplicationRequest scaleApplicationRequest = ScaleApplicationRequest.builder()
                                                                                 .name(applicationName)
                                                                                 .instances(instances)
                                                                                 .build();
        cloudFoundryOperations.applications().scale(scaleApplicationRequest).block();
    }
}
