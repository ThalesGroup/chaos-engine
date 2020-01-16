/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.container.impl;

import com.thales.chaos.container.enums.CloudFoundryApplicationRouteType;
import com.thales.chaos.exception.ChaosException;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.domains.Status;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.*;

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
    private Domain httpDomain2 = Domain.builder()
                                       .id("httpDomain2")
                                       .name("http.domain.com")
                                       .type(null)
                                       .status(Status.SHARED)
                                       .build();
    private Domain tcpDomain = Domain.builder()
                                     .id("tcpDomain")
                                     .name("tcp.domain.com")
                                     .type("tcp").status(Status.SHARED).build();
    private Domain unknownDomainType = Domain.builder()
                                             .id("unknownDomain")
                                             .name("unknownDomain.domain.com")
                                             .type("unknown")
                                             .status(Status.SHARED)
                                             .build();
    private RouteEntity httpRouteEntity = RouteEntity.builder().host("httpHost").domainId(httpDomain.getId()).build();
    private RouteEntity tcpRouteEntity = RouteEntity.builder().port(666).domainId(tcpDomain.getId()).build();
    private RouteEntity unknownRouteEntity = RouteEntity.builder()
                                                        .host("unknownRouteEntity")
                                                        .domainId(unknownDomainType.getId())
                                                        .build();
    private CloudFoundryApplicationRoute httpRoute;
    private CloudFoundryApplicationRoute httpRoute2;
    private CloudFoundryApplicationRoute tcpRoute;
    private CloudFoundryApplicationRoute unknownRouteType;

    @Before
    public void setUp () {
        httpRoute = CloudFoundryApplicationRoute.builder()
                                                .applicationName(APP1_NAME)
                                                .domain(httpDomain)
                                                .route(httpRouteEntity)
                                                .build();
        httpRoute2 = CloudFoundryApplicationRoute.builder()
                                                 .applicationName(APP1_NAME)
                                                 .domain(httpDomain2)
                                                 .route(httpRouteEntity)
                                                 .build();
        tcpRoute = CloudFoundryApplicationRoute.builder()
                                               .applicationName(APP2_NAME)
                                               .domain(tcpDomain)
                                               .route(tcpRouteEntity)
                                               .build();
        unknownRouteType = CloudFoundryApplicationRoute.builder()
                                                       .applicationName(APP2_NAME)
                                                       .domain(unknownDomainType)
                                                       .route(unknownRouteEntity)
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
        assertEquals(expectedMapRouteRequest, httpRoute2.getMapRouteRequest());
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

    @Test(expected = ChaosException.class)
    public void unknownRouteMap () {
        unknownRouteType.getMapRouteRequest();
    }

    @Test
    public void httpRouteUnMap () {
        UnmapRouteRequest expectedUnMapRouteRequest = UnmapRouteRequest.builder()
                                                                       .applicationName(APP1_NAME)
                                                                       .domain(httpDomain.getName())
                                                                       .host(httpRouteEntity.getHost())
                                                                       .path(httpRouteEntity.getPath())
                                                                       .port(httpRouteEntity.getPort())
                                                                       .build();
        assertEquals(expectedUnMapRouteRequest, httpRoute.getUnmapRouteRequest());
        assertEquals(expectedUnMapRouteRequest, httpRoute2.getUnmapRouteRequest());
    }

    @Test
    public void tcpRouteUnMap () {
        UnmapRouteRequest expectedUnMapRouteRequest = UnmapRouteRequest.builder()
                                                                       .applicationName(APP2_NAME)
                                                                       .domain(tcpDomain.getName())
                                                                       .host(tcpRouteEntity.getHost())
                                                                       .path(tcpRouteEntity.getPath())
                                                                       .port(tcpRouteEntity.getPort())
                                                                       .build();
        assertEquals(expectedUnMapRouteRequest, tcpRoute.getUnmapRouteRequest());
    }

    @Test(expected = ChaosException.class)
    public void unknownRouteUnMap () {
        unknownRouteType.getUnmapRouteRequest();
    }

    @Test
    public void testToString () {
        assertThat(httpRoute.toString(), CoreMatchers.containsString("type=HTTP"));
        assertThat(tcpRoute.toString(), CoreMatchers.containsString("type=TCP"));
    }

    @Test
    public void testRouteType () {
        assertEquals(CloudFoundryApplicationRouteType.HTTP, httpRoute.getType());
        assertEquals(CloudFoundryApplicationRouteType.HTTP, httpRoute2.getType());
        assertEquals(CloudFoundryApplicationRouteType.TCP, tcpRoute.getType());
    }

    @Test
    public void testGetHost () {
        assertEquals(httpRouteEntity.getHost(), httpRoute.getHost());
        assertEquals(httpRouteEntity.getHost(), httpRoute2.getHost());
        assertNull(tcpRouteEntity.getHost());
    }
}