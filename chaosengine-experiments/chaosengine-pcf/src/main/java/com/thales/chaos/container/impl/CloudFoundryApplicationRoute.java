package com.thales.chaos.container.impl;

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
