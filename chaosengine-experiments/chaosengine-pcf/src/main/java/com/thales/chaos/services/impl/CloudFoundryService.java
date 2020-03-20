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

package com.thales.chaos.services.impl;

import com.thales.chaos.services.CloudService;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.*;
import java.util.List;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "cf")
@ConditionalOnProperty({ "cf.apihost" })
public class CloudFoundryService implements CloudService {
    private String apiHost;
    private Integer port = 443;
    private String username;
    private String password;
    private String organization;
    private String space = "default";

    public void setApiHost (String apiHost) {
        this.apiHost = apiHost;
    }

    public void setPort (Integer port) {
        this.port = port;
    }

    public void setUsername (String username) {
        this.username = username;
    }

    public void setPassword (String password) {
        this.password = password;
    }

    public void setOrganization (String organization) {
        this.organization = organization;
    }

    public void setSpace (String space) {
        this.space = space;
    }

    @Bean
    @RefreshScope
    ConnectionContext connectionContext () {
        DefaultConnectionContext.Builder connectionContextBuilder;
        connectionContextBuilder = DefaultConnectionContext.builder().apiHost(apiHost).port(port);
        proxyConfiguration().ifPresent(connectionContextBuilder::proxyConfiguration);
        return connectionContextBuilder.build();
    }

    Optional<ProxyConfiguration> proxyConfiguration () {
        List<Proxy> proxies = ProxySelector.getDefault().select(URI.create("https://" + apiHost + ":" + port));
        if (proxies == null || proxies.isEmpty() || proxies.get(0).equals(Proxy.NO_PROXY)) {
            return Optional.empty();
        }
        Proxy proxy = proxies.get(0);
        SocketAddress proxyAddress = proxy.address();
        if (!(proxyAddress instanceof InetSocketAddress)) {
            return Optional.empty();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) proxyAddress;
        String proxyHost = inetSocketAddress.getHostString();
        int proxyPort = inetSocketAddress.getPort();
        return Optional.of(ProxyConfiguration.builder().host(proxyHost).port(proxyPort).build());
    }

    @Bean
    @RefreshScope
    TokenProvider tokenProvider () {
        return PasswordGrantTokenProvider.builder().password(password).username(username).build();
    }

    @Bean
    @RefreshScope
    CloudFoundryClient cloudFoundryClient (ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    @Bean
    @RefreshScope
    DopplerClient dopplerClient (ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorDopplerClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    @Bean
    @RefreshScope
    UaaClient uaaClient (ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorUaaClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    @Bean
    @RefreshScope
    CloudFoundryOperations cloudFoundryOperations (CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient, UaaClient uaaClient) {
        return DefaultCloudFoundryOperations.builder()
                                            .cloudFoundryClient(cloudFoundryClient)
                                            .dopplerClient(dopplerClient)
                                            .uaaClient(uaaClient)
                                            .organization(organization)
                                            .space(space)
                                            .build();
    }
}
