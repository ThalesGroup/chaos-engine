package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.CloudFoundryApplication;
import com.gemalto.chaos.container.impl.CloudFoundryApplicationRoute;
import com.gemalto.chaos.selfawareness.CloudFoundrySelfAwareness;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.gemalto.chaos.constants.CloudFoundryConstants.*;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryApplicationPlatformTest {
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
    @MockBean
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    @MockBean
    private CloudFoundryOperations cloudFoundryOperations;
    @MockBean
    private ContainerManager containerManager;
    @Mock
    private Applications applications;
    @Mock
    private Routes routes;
    @MockBean
    private CloudFoundryClient cloudFoundryClient;
    @MockBean
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;
    private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    private Route httpRoute = Route.builder()
                                   .application(APPLICATION_NAME)
                                   .host("host")
                                   .domain("domain")
                                   .id("id")
                                   .space("space")
                                   .build();
    private Route tcpRoute = Route.builder()
                                  .application(APPLICATION_NAME)
                                  .type("tcp")
                                  .domain("domain")
                                  .port("1")
                                  .id("id")
                                  .host("")
                                  .space("space")
                                  .build();
    private CloudFoundryApplicationRoute app1_httpRoute;
    private CloudFoundryApplicationRoute app1_tcpRoute;

    @Before
    public void setUp () {
        cloudFoundryApplicationPlatform = new CloudFoundryApplicationPlatform(cloudFoundryOperations, cloudFoundryClient, cloudFoundryPlatformInfo);
        app1_httpRoute = CloudFoundryApplicationRoute.builder().fromRoute(httpRoute).build();
        app1_tcpRoute = CloudFoundryApplicationRoute.builder().fromRoute(tcpRoute).build();
        List<CloudFoundryApplicationRoute> app1_routes = new ArrayList<>();
        app1_routes.add(app1_httpRoute);
        app1_routes.add(app1_tcpRoute);

        EXPECTED_CONTAINER_1 = CloudFoundryApplication.builder()
                                                      .containerInstances(INSTANCES)
                                                      .applicationID(APPLICATION_ID)
                                                      .platform(cloudFoundryApplicationPlatform)
                                                      .name(APPLICATION_NAME).applicationRoutes(app1_routes)
                                                      .build();
        EXPECTED_CONTAINER_2 = CloudFoundryApplication.builder()
                                                      .containerInstances(INSTANCES)
                                                      .applicationID(APPLICATION_ID_2)
                                                      .platform(cloudFoundryApplicationPlatform)
                                                      .name(APPLICATION_NAME_2).applicationRoutes(new ArrayList<>())
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
    public void getRoster () {
        ApplicationSummary stoppedApplicationSummary = builder_1.requestedState(CLOUDFOUNDRY_APPLICATION_STOPPED)
                                                                .build();
        ApplicationSummary zeroInstancesApplicationSummary = builder_1.instances(0).build();
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1, applicationSummary_2, stoppedApplicationSummary, zeroInstancesApplicationSummary);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        Flux<Route> routeFlux = Flux.just(httpRoute, tcpRoute);
        doReturn(routes).when(cloudFoundryOperations).routes();
        doReturn(routeFlux).when(routes).list(any());
        assertThat(cloudFoundryApplicationPlatform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(EXPECTED_CONTAINER_1, EXPECTED_CONTAINER_2));
    }

    @Test
    public void checkPlatformHealthNormal () {
        Integer INSTANCE_ID = 0;
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        ApplicationInstancesResponse applicationInstancesResponse = ApplicationInstancesResponse.builder()
                                                                                                .instance(INSTANCE_ID.toString(), ApplicationInstanceInfo
                                                                                                        .builder()
                                                                                                        .consoleIp("")
                                                                                                        .consolePort(22)
                                                                                                        .debugIp("")
                                                                                                        .debugPort(8888)
                                                                                                        .details("")
                                                                                                        .since(0D)
                                                                                                        .state(CLOUDFOUNDRY_RUNNING_STATE)
                                                                                                        .uptime(0L)
                                                                                                        .build())
                                                                                                .build();
        Mono<ApplicationInstancesResponse> applicationInstancesResponseMono = Mono.just(applicationInstancesResponse);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        doReturn(applicationInstancesResponseMono).when(applicationsV2)
                                                  .instances(any(ApplicationInstancesRequest.class));
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertEquals(ContainerHealth.NORMAL, cloudFoundryApplicationPlatform.checkPlatformHealth());
    }

    @Test
    public void checkPlatformHealthConainerInstanceStarting () {
        Integer INSTANCE_ID = 0;
        ApplicationsV2 applicationsV2 = mock(ApplicationsV2.class);
        ApplicationInstancesResponse applicationInstancesResponse = ApplicationInstancesResponse.builder()
                                                                                                .instance(INSTANCE_ID.toString(), ApplicationInstanceInfo
                                                                                                        .builder()
                                                                                                        .consoleIp("")
                                                                                                        .consolePort(22)
                                                                                                        .debugIp("")
                                                                                                        .debugPort(8888)
                                                                                                        .details("")
                                                                                                        .since(0D)
                                                                                                        .state(CLOUDFOUNDRY_STARTING_STATE)
                                                                                                        .uptime(0L)
                                                                                                        .build())
                                                                                                .build();
        Mono<ApplicationInstancesResponse> applicationInstancesResponseMono = Mono.just(applicationInstancesResponse);
        doReturn(applicationsV2).when(cloudFoundryClient).applicationsV2();
        doReturn(applicationInstancesResponseMono).when(applicationsV2)
                                                  .instances(any(ApplicationInstancesRequest.class));
        Flux<ApplicationSummary> applicationsFlux = Flux.just(applicationSummary_1);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(applicationsFlux).when(applications).list();
        assertEquals(ContainerHealth.UNDER_ATTACK, cloudFoundryApplicationPlatform.checkPlatformHealth());
    }

    @Test
    public void rescaleApplication () {
        ScaleApplicationRequest scaleApplicationRequest = ScaleApplicationRequest.builder()
                                                                                 .name(APPLICATION_NAME)
                                                                                 .instances(INSTANCES)
                                                                                 .build();
        Mono monoVoid = mock(Mono.class);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(monoVoid).when(applications).scale(scaleApplicationRequest);
        cloudFoundryApplicationPlatform.rescaleApplication(APPLICATION_NAME, INSTANCES);
        verify(applications, times(1)).scale(scaleApplicationRequest);
        verify(monoVoid, times(1)).block();
    }

    @Test
    public void restartApplication () {
        RestartApplicationRequest restartApplicationRequest = RestartApplicationRequest.builder()
                                                                                       .name(APPLICATION_NAME)
                                                                                       .build();
        Mono monoVoid = mock(Mono.class);
        doReturn(applications).when(cloudFoundryOperations).applications();
        doReturn(monoVoid).when(applications).restart(restartApplicationRequest);
        cloudFoundryApplicationPlatform.restartApplication(APPLICATION_NAME);
        verify(applications, times(1)).restart(restartApplicationRequest);
        verify(monoVoid, times(1)).block();
    }
}