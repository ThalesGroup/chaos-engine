package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.impl.CloudFoundryApplication;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;

import java.util.Collections;

import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STARTED;
import static com.gemalto.chaos.constants.CloudFoundryConstants.CLOUDFOUNDRY_APPLICATION_STOPPED;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryPlatformTest {
    @MockBean
    private CloudFoundryOperations cloudFoundryOperations;
    @MockBean
    private ContainerManager containerManager;
    @Mock
    private Applications applications;
    @MockBean
    private CloudFoundryClient cloudFoundryClient;
    @MockBean
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;
    @Autowired
    private CloudFoundryPlatform cfp;
    private String APPLICATION_NAME = randomUUID().toString();
    private String APPLICATION_ID = randomUUID().toString();
    private String APPLICATION_NAME_2 = randomUUID().toString();
    private String APPLICATION_ID_2 = randomUUID().toString();
    private Integer INSTANCES = 2;
    private CloudFoundryApplication EXPECTED_CONTAINER_1;
    private CloudFoundryApplication EXPECTED_CONTAINER_2;
    private ApplicationSummary.Builder builder_1;
    private ApplicationSummary.Builder builder_2;
    private ApplicationSummary applicationSummary_1;
    private ApplicationSummary applicationSummary_2;

    @Before
    public void setUp () {
        CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform = new CloudFoundryApplicationPlatform();
        EXPECTED_CONTAINER_1 = CloudFoundryApplication.builder()
                                                      .containerInstances(INSTANCES)
                                                      .applicationID(APPLICATION_ID)
                                                      .platform(cloudFoundryApplicationPlatform)
                                                      .name(APPLICATION_NAME)
                                                      .build();
        EXPECTED_CONTAINER_2 = CloudFoundryApplication.builder()
                                                      .containerInstances(INSTANCES)
                                                      .applicationID(APPLICATION_ID_2)
                                                      .platform(cloudFoundryApplicationPlatform)
                                                      .name(APPLICATION_NAME_2)
                                                      .build();
        builder_1 = ApplicationSummary.builder()
                                      .diskQuota(0)
                                      .instances(INSTANCES)
                                      .id(APPLICATION_ID).name(APPLICATION_NAME).addAllUrls(Collections.emptySet())
                                      .runningInstances(INSTANCES)
                                      .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                      .memoryLimit(0);
        builder_2 = ApplicationSummary.builder()
                                      .diskQuota(0)
                                      .instances(INSTANCES)
                                      .id(APPLICATION_ID_2).name(APPLICATION_NAME_2).addAllUrls(Collections.emptySet())
                                      .runningInstances(INSTANCES)
                                      .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                      .memoryLimit(0);
        applicationSummary_1 = builder_1.build();
        applicationSummary_2 = builder_2.build();
    }

    @Test
    public void getApiStatusError () {
        when(cloudFoundryOperations.applications()).thenThrow(mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, cfp.getApiStatus());
    }

    @Test
    public void getApiStatusSuccess () {
        when(cloudFoundryOperations.applications()).thenReturn(applications);
        assertEquals(ApiStatus.OK, cfp.getApiStatus());
    }

    @Test
    public void getPlatformHealth () {
        ApplicationSummary stoppedApplicationSummary = builder_1.requestedState(CLOUDFOUNDRY_APPLICATION_STOPPED)
                                                                .build();
        ApplicationSummary zeroInstancesApplicationSummary = builder_1.instances(0).build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1, applicationSummary_2, stoppedApplicationSummary, zeroInstancesApplicationSummary);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertEquals(PlatformHealth.OK, cfp.getPlatformHealth());
    }

    @Test
    public void getPlatformDegradated () {
        applicationSummary_1 = builder_1.instances(10)
                                        .runningInstances(1)
                                        .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                        .build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1, applicationSummary_2);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertEquals(PlatformHealth.DEGRADED, cfp.getPlatformHealth());
    }

    @Test
    public void getPlatformFailed () {
        applicationSummary_1 = builder_1.instances(10)
                                        .runningInstances(0)
                                        .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                        .build();
        applicationSummary_2 = builder_2.instances(20)
                                        .runningInstances(0)
                                        .requestedState(CLOUDFOUNDRY_APPLICATION_STARTED)
                                        .build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1, applicationSummary_2);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertEquals(PlatformHealth.FAILED, cfp.getPlatformHealth());
    }

    @Test
    public void getPlatformLevel () {
        assertEquals(PlatformLevel.PAAS, cfp.getPlatformLevel());
    }

    @Test
    public void generateRoster () {
        assertNotNull(cfp.generateRoster());
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        CloudFoundryPlatform cloudFoundryPlatform () {
            CloudFoundryPlatform platform = new CloudFoundryPlatform() {
                @Override
                public boolean isContainerRecycled (Container container) {
                    return false;
                }
            };
            return spy(platform);
        }
    }
}
