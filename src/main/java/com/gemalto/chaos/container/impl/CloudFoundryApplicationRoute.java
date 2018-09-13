package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType;

import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.http;

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

        public CloudFoundryApplicationRouteBuilder host (String host) {
            this.host = host;
        }

        public CloudFoundryApplicationRouteBuilder domain (String domain) {
            this.domain = domain;
        }

        public CloudFoundryApplicationRouteBuilder port (int port) {
            this.port = port;
        }

        public CloudFoundryApplicationRouteBuilder path (String path) {
            this.path = path;
        }

        public CloudFoundryApplicationRouteBuilder type (CloudFoundryApplicationRouteType type) {
            this.type = type;
        }

        public CloudFoundryApplicationRouteBuilder applicationName (String applicationName) {
            this.applicationName = applicationName;
        }

        public CloudFoundryApplicationRouteBuilder service (String service) {
            this.service = service;
        }
    }
}
