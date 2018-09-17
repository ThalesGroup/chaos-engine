package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;

import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.http;
import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.tcp;

public class CloudFoundryApplicationRoute {
    private RouteEntity route;
    private Domain domain;
    private String applicationName;
    private String host;
    private String path;
    private Integer port;
    private CloudFoundryApplicationRouteType type = http;

    public static CloudFoundryApplicationRouteBuilder builder () {
        return CloudFoundryApplicationRouteBuilder.builder();
    }

    public UnmapRouteRequest getUnmapRouteRequest () {
        if (type == http) {
            return UnmapRouteRequest.builder()
                                    .applicationName(applicationName)
                                    .domain(domain.getName())
                                    .path(path)
                                    .host(host)
                                    .build();
        } else {
            return UnmapRouteRequest.builder()
                                    .applicationName(applicationName)
                                    .domain(domain.getName())
                                    .port(port)
                                    .build();
        }
    }

    public MapRouteRequest getMapRouteRequest () {
        if (type == http) {
            return MapRouteRequest.builder()
                                  .applicationName(applicationName)
                                  .domain(domain.getName())
                                  .path(path)
                                  .host(host)
                                  .build();
        } else {
            return MapRouteRequest.builder()
                                  .applicationName(applicationName)
                                  .domain(domain.getName())
                                  .port(port)
                                  .build();
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
            cloudFoundryApplicationRoute.route = route;
            cloudFoundryApplicationRoute.domain = domain;
            cloudFoundryApplicationRoute.host = route.getHost();
            cloudFoundryApplicationRoute.path = route.getPath();
            cloudFoundryApplicationRoute.port = route.getPort();
            if (tcp.toString().equals(domain.getType())) {
                cloudFoundryApplicationRoute.type = tcp;
            }

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
