package com.gemalto.chaos.platform;

import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty({"cf.apihost", "cf.username", "cf.password"})
public class CloudFoundryService implements Platform {

    private static final Logger log = LoggerFactory.getLogger(CloudFoundryService.class);

    @Bean
    DefaultConnectionContext defaultConnectionContext(@Value("${cf.apihost}") String apiHost) {
        return DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();
    }

    @Bean
    PasswordGrantTokenProvider tokenProvider(@Value("${cf.username}") String username,
                                             @Value("${cf.password}") String password) {
        return PasswordGrantTokenProvider.builder()
                .password(password)
                .username(username)
                .build();
    }

    @Autowired
    public CloudFoundryService() {
        log.info("Initialized!");
    }

}
