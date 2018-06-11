package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.CloudFoundryContainer;
import com.gemalto.chaos.container.Container;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@ConditionalOnProperty({"cf.apihost", "cf.username", "cf.password"})
public class CloudFoundryService implements Platform {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryService.class);

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

    public CloudFoundryService() {
        log.info("Initialized!");
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
        return null;

    }

    @Override
    public void destroy(Container container) throws RuntimeException {
        if (!(container instanceof CloudFoundryContainer)) {
            throw new RuntimeException("Expected to be passed a Cloud Foundry container");
        }

    }

}
