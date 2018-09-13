package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType;

import static com.gemalto.chaos.container.enums.CloudFoundryApplicationRouteType.http;

public class CloudFoundryApplicationRoute {
    private String host;
    private String domain;
    private int port;
    private String path;
    private CloudFoundryApplicationRouteType type = http;
    private String application;
    private String service;

    public static final class CloudFoundryApplicationRouteBuilder {
        private String host;
        private String domain;
        private int port;
        private String path;
        private CloudFoundryApplicationRouteType type = http;
        private String application;
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
            cloudFoundryApplicationRoute.application = this.application;
            cloudFoundryApplicationRoute.service = this.service;
            return cloudFoundryApplicationRoute;
        }

        public void host (String host) {
            this.host = host;
        }

        public void domain (String domain) {
            this.domain = domain;
        }

        public void port (int port) {
            this.port = port;
        }

        public void path (String path) {
            this.path = path;
        }

        public void type (CloudFoundryApplicationRouteType type) {
            this.type = type;
        }

        public void application (String application) {
            this.application = application;
        }

        public void service (String service) {
            this.service = service;
        }
    }
}
