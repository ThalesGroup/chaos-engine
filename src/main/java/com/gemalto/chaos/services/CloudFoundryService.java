package com.gemalto.chaos.services;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({"cf_apihost"})
public class CloudFoundryService {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryService.class);

    @Bean
    DefaultConnectionContext defaultConnectionContext(@Value("${cf_apihost}") String apiHost,
                                                      @Value("${cf_port:443}") int port) {
        log.info("Creating a connection context");
        return DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .port(port)
                .build();
    }

    @Bean
    PasswordGrantTokenProvider tokenProvider(@Value("${cf_username}") String username,
                                             @Value("${cf_password}") String password) {
        log.info("Creating a token provider");
        return PasswordGrantTokenProvider.builder()
                .password(password)
                .username(username)
                .build();
    }

    @Bean
    ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    @Bean
    ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorDopplerClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    @Bean
    ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorUaaClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    @Bean
    @ConditionalOnProperty({"cf_organization"})
    DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
                                                         DopplerClient dopplerClient,
                                                         UaaClient uaaClient,
                                                         @Value("${cf_organization}") String organization,
                                                         @Value("${cf_space:default}") String space) {
        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .dopplerClient(dopplerClient)
                .uaaClient(uaaClient)
                .organization(organization)
                .space(space)
                .build();
    }
}
