package com.thales.chaos.container.impl;

import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.domains.Status;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryApplicationRouteTest {
    private final String APP1_NAME = UUID.randomUUID().toString();
    private final String APP2_NAME = UUID.randomUUID().toString();
    private Domain httpDomain = Domain.builder()
                                      .id("httpDomain")
                                      .name("http.domain.com")
                                      .type("")
                                      .status(Status.SHARED)
                                      .build();
    private Domain tcpDomain = Domain.builder()
                                     .id("tcpDomain")
                                     .name("tcp.domain.com")
                                     .type("tcp")
                                     .status(Status.SHARED)
                                     .build();
    private RouteEntity httpRouteEntity = RouteEntity.builder().host("httpHost").domainId(httpDomain.getId()).build();
    private RouteEntity tcpRouteEntity = RouteEntity.builder().port(666).domainId(tcpDomain.getId()).build();
    private CloudFoundryApplicationRoute httpRoute;
    private CloudFoundryApplicationRoute tcpRoute;

    @Before
    public void setUp () {
        httpRoute = CloudFoundryApplicationRoute.builder()
                                                .applicationName(APP1_NAME)
                                                .domain(httpDomain)
                                                .route(httpRouteEntity)
                                                .build();
        tcpRoute = CloudFoundryApplicationRoute.builder()
                                               .applicationName(APP2_NAME)
                                               .domain(tcpDomain)
                                               .route(tcpRouteEntity)
                                               .build();
    }

    @Test
    public void httpRouteMap () {
        MapRouteRequest expectedMapRouteRequest = MapRouteRequest.builder()
                                                                 .applicationName(APP1_NAME)
                                                                 .domain(httpDomain.getName())
                                                                 .host(httpRouteEntity.getHost())
                                                                 .path(httpRouteEntity.getPath())
                                                                 .port(httpRouteEntity.getPort())
                                                                 .build();
        assertEquals(expectedMapRouteRequest, httpRoute.getMapRouteRequest());
    }

    @Test
    public void tcpRouteMap () {
        MapRouteRequest expectedMapRouteRequest = MapRouteRequest.builder()
                                                                 .applicationName(APP2_NAME)
                                                                 .domain(tcpDomain.getName())
                                                                 .host(tcpRouteEntity.getHost())
                                                                 .path(tcpRouteEntity.getPath())
                                                                 .port(tcpRouteEntity.getPort())
                                                                 .build();
        assertEquals(expectedMapRouteRequest, tcpRoute.getMapRouteRequest());
    }

    @Test
    public void httpRouteUnMap () {
        UnmapRouteRequest expectedMapRouteRequest = UnmapRouteRequest.builder()
                                                                     .applicationName(APP1_NAME)
                                                                     .domain(httpDomain.getName())
                                                                     .host(httpRouteEntity.getHost())
                                                                     .path(httpRouteEntity.getPath())
                                                                     .port(httpRouteEntity.getPort())
                                                                     .build();
        assertEquals(expectedMapRouteRequest, httpRoute.getUnmapRouteRequest());
    }

    @Test
    public void tcpRouteUnMap () {
        UnmapRouteRequest expectedMapRouteRequest = UnmapRouteRequest.builder()
                                                                     .applicationName(APP2_NAME)
                                                                     .domain(tcpDomain.getName())
                                                                     .host(tcpRouteEntity.getHost())
                                                                     .path(tcpRouteEntity.getPath())
                                                                     .port(tcpRouteEntity.getPort())
                                                                     .build();
        assertEquals(expectedMapRouteRequest, tcpRoute.getUnmapRouteRequest());

    }

    @Test
    public void testToString () {
        assertTrue(httpRoute.toString().contains("type=HTTP"));
        assertTrue(tcpRoute.toString().contains("type=TCP"));
    }
}