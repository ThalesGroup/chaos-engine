package com.gemalto.chaos.platform.impl;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty({ "cf.organization" })
public class CloudFoundryPlatformInfoFactory {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPlatformInfoFactory.class);
    @Autowired
    private CloudFoundryClient cloudFoundryClient;
    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;

    CloudFoundryPlatformInfoFactory (CloudFoundryClient cloudFoundryClient, CloudFoundryOperations cloudFoundryOperations) {
        this.cloudFoundryClient = cloudFoundryClient;
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    @Bean
    CloudFoundryPlatformInfo getCloudFoundryPlatformInfo () {
        Mono<GetInfoResponse> infoResponse = cloudFoundryClient.info().get(GetInfoRequest.builder().build());
        Mono<String> sshCodeResponse = cloudFoundryOperations.advanced().sshCode();
        return CloudFoundryPlatformInfo.builder()
                                       .setInfoResponse(infoResponse)
                                       .setSshCodeResponse(sshCodeResponse)
                                       .build();
    }
}
