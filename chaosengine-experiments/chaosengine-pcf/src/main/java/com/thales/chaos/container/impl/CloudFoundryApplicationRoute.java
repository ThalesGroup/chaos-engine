/*
 *    Copyright (c) 2019 Thales Group
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.container.enums.CloudFoundryApplicationRouteType;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.CloudFoundryChaosErrorCode;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;

import static com.thales.chaos.container.enums.CloudFoundryApplicationRouteType.UNKNOWN;

public class CloudFoundryApplicationRoute {
    private Domain domain;
    private String applicationName;
    private String host;
    private String path;
    private Integer port;
    private CloudFoundryApplicationRouteType type = UNKNOWN;

    public static CloudFoundryApplicationRouteBuilder builder () {
        return CloudFoundryApplicationRouteBuilder.builder();
    }

    public String getHost () {
        return host;
    }

    public CloudFoundryApplicationRouteType getType () {
        return type;
    }

    @JsonIgnore
    public UnmapRouteRequest getUnmapRouteRequest () {
        UnmapRouteRequest.Builder builder = UnmapRouteRequest.builder();
        builder.applicationName(applicationName).domain(domain.getName());
        switch (type) {
            case HTTP:
                return builder.path(path).host(host).build();
            case TCP:
                return builder.port(port).build();
            default:
                throw new ChaosException(CloudFoundryChaosErrorCode.UNSUPPORTED_ROUTE_TYPE);
        }
    }

    @JsonIgnore
    public MapRouteRequest getMapRouteRequest () {
        MapRouteRequest.Builder builder = MapRouteRequest.builder();
        builder.applicationName(applicationName).domain(domain.getName());
        switch (type) {
            case HTTP:
                return builder.path(path).host(host).build();
            case TCP:
                return builder.port(port).build();
            default:
                throw new ChaosException(CloudFoundryChaosErrorCode.UNSUPPORTED_ROUTE_TYPE);
        }
    }

    @Override
    public String toString () {
        return "Route{" + "application=" + applicationName + ", domain=" + domain.getName() + ", host=" + host + ", path=" + path + ", port=" + port + ", type=" + type + "}";
    }

    public static final class CloudFoundryApplicationRouteBuilder {
        private RouteEntity route;
        private Domain domain;
        private String applicationName;

        static CloudFoundryApplicationRouteBuilder builder () {
            return new CloudFoundryApplicationRouteBuilder();
        }

        public CloudFoundryApplicationRoute build () {
            CloudFoundryApplicationRoute cloudFoundryApplicationRoute = new CloudFoundryApplicationRoute();
            cloudFoundryApplicationRoute.applicationName = applicationName;
            cloudFoundryApplicationRoute.domain = domain;
            cloudFoundryApplicationRoute.host = route.getHost();
            cloudFoundryApplicationRoute.path = route.getPath();
            cloudFoundryApplicationRoute.port = route.getPort();
            cloudFoundryApplicationRoute.type = CloudFoundryApplicationRouteType.mapFromString(domain.getType());
            return cloudFoundryApplicationRoute;
        }

        public CloudFoundryApplicationRouteBuilder route (RouteEntity route) {
            this.route = route;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder domain (Domain domain) {
            this.domain = domain;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder applicationName (String applicationName) {
            this.applicationName = applicationName;
            return this;
        }
    }
}
