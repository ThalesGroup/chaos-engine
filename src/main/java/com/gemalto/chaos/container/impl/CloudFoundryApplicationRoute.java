package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType;
import org.cloudfoundry.operations.routes.Route;

import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.http;
import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.tcp;

public class CloudFoundryApplicationRoute {
    private String host;
    private String domain;
    private int port;
    private String path;
    private CloudFoundryApplicationRouteType type = http;
    private String applicationName;
    private String service;

    public static CloudFoundryApplicationRouteBuilder builder () {
        return CloudFoundryApplicationRouteBuilder.builder();
    }

    public static final class CloudFoundryApplicationRouteBuilder {
        private String host;
        private String domain;
        private int port;
        private String path;
        private CloudFoundryApplicationRouteType type = http;
        private String applicationName;
        private String service;

        static CloudFoundryApplicationRouteBuilder builder () {
            return new CloudFoundryApplicationRouteBuilder();
        }

        public CloudFoundryApplicationRoute build () {
            CloudFoundryApplicationRoute cloudFoundryApplicationRoute = new CloudFoundryApplicationRoute();
            cloudFoundryApplicationRoute.host = this.host;
            cloudFoundryApplicationRoute.domain = this.domain;
            cloudFoundryApplicationRoute.port = this.port;
            cloudFoundryApplicationRoute.path = this.path;
            cloudFoundryApplicationRoute.type = this.type;
            cloudFoundryApplicationRoute.applicationName = this.applicationName;
            cloudFoundryApplicationRoute.service = this.service;
            return cloudFoundryApplicationRoute;
        }

        public CloudFoundryApplicationRouteBuilder fromRoute (Route route) {
            domain = route.getDomain();
            applicationName = route.getApplications().get(0);
            service = route.getService();
            if (route.getType() == tcp.name()) {
                type = tcp;
                port = Integer.valueOf(route.getPort());
            } else {
                type = http;
                path = route.getPath();
                host = route.getHost();
            }
            return this;
        }

        public CloudFoundryApplicationRouteBuilder host (String host) {
            this.host = host;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder domain (String domain) {
            this.domain = domain;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder port (int port) {
            this.port = port;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder path (String path) {
            this.path = path;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder type (CloudFoundryApplicationRouteType type) {
            this.type = type;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder applicationName (String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public CloudFoundryApplicationRouteBuilder service (String service) {
            this.service = service;
            return this;
        }
    }
}
