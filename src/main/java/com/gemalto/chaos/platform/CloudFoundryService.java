package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.CloudFoundryContainer;
import com.gemalto.chaos.container.Container;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty({"cf.apihost", "cf.username", "cf.password", "cf.organization", "cf.space"})
public class CloudFoundryService implements Platform {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryService.class);

    @Autowired
    private DefaultCloudFoundryOperations cloudFoundryOperations;

    public CloudFoundryService() {
        log.info("Initialized!");
    }

    @Bean
    DefaultConnectionContext defaultConnectionContext(@Value("${cf.apihost}") String apiHost) {
        log.info("Creating a connection context");
        return DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();
    }

    @Bean
    PasswordGrantTokenProvider tokenProvider(@Value("${cf.username}") String username,
                                             @Value("${cf.password}") String password) {
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
    @ConditionalOnProperty({"cf.organization", "cf.space"})
    DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
                                                         DopplerClient dopplerClient,
                                                         UaaClient uaaClient,
                                                         @Value("${cf.organization}") String organization,
                                                         @Value("${cf.space}") String space) {
        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .dopplerClient(dopplerClient)
                .uaaClient(uaaClient)
                .organization(organization)
                .space(space)
                .build();
    }

    @Override
    public void degrade(Container container) throws RuntimeException {
        if (!(container instanceof CloudFoundryContainer)) {
            throw new RuntimeException("Expected to be passed a Cloud Foundry container");
        }
        log.info("Attempting to degrade performance on {}", container);

    }

    @Override
    public List<Container> getRoster() {
        List<Container> containers = new ArrayList<>();
        Flux<ApplicationSummary> apps = cloudFoundryOperations.applications().list();
        for (ApplicationSummary app : apps.toIterable()) {
            Integer instances = app.getInstances();
            for (Integer i = 0; i < instances; i++) {
                containers.add(
                        CloudFoundryContainer.builder()
                                .applicationId(app.getId())
                                .name(app.getName())
                                .instance(i)
                                .build()
                );
            }
        }
        return containers;
    }

    @Override
    public void destroy(Container container) throws RuntimeException {
        if (!(container instanceof CloudFoundryContainer)) {
            throw new RuntimeException("Expected to be passed a Cloud Foundry container");
        }

        cloudFoundryOperations.applications().terminateTask(
                ((CloudFoundryContainer) container).getTerminateApplicationTaskRequest());


    }

}
