package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryApplication;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;

@Component
@ConditionalOnProperty({ "cf_organization" })
public class CloudFoundryApplicationPlatform extends CloudFoundryPlatform {
    private CloudFoundryOperations cloudFoundryOperations;

    @Autowired
    public CloudFoundryApplicationPlatform (CloudFoundryOperations cloudFoundryOperations, CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryOperations, cloudFoundryPlatformInfo);
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    public ContainerHealth checkApplicationHealth (String applicationName) {
        return ContainerHealth.UNDER_ATTACK;
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
                                                                     .name(app.getName())
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
