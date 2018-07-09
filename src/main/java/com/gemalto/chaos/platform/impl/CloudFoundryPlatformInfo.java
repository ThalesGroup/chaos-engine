package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.platform.PlatformInfo;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

//@Component
public class CloudFoundryPlatformInfo implements PlatformInfo {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPlatformInfo.class);
    private String sshCode;
    private String applicationSshEndpoint;
    private String applicationSshHostKeyFingerprint;
    private String applicationSshPort;
    private String platformVersion;
    private Mono<String> sshCodeResponse;
    private Mono<GetInfoResponse> infoResponse;

    public static CloudFoundryPlatformInfoBuilder builder () {
        return CloudFoundryPlatformInfoBuilder.builder();
    }

    public void fetchInfo () {
        GetInfoResponse response = infoResponse.block();
        log.debug("Fetching Cloud Foundry Info");
        String sshEndpoint[] = response.getApplicationSshEndpoint().split(":");
        this.applicationSshEndpoint = sshEndpoint[0];
        this.applicationSshPort = sshEndpoint[1];
        this.applicationSshHostKeyFingerprint = response.getApplicationSshHostKeyFingerprint();
        this.platformVersion = response.getApiVersion();
        this.sshCode = sshCodeResponse.block();
    }

    public String getApplicationSshEndpoint () {
        return applicationSshEndpoint;
    }

    public String getSshCode () {
        return sshCode;
    }

    public String getApplicationSshHostKeyFingerprint () {
        return applicationSshHostKeyFingerprint;
    }

    public String getApplicationSshPort () {
        return applicationSshPort;
    }

    @Override
    public String getPlatformVersion () {
        return platformVersion;
    }

    public static final class CloudFoundryPlatformInfoBuilder {
        private Mono<String> sshCodeResponse;
        private Mono<GetInfoResponse> infoResponse;

        static CloudFoundryPlatformInfoBuilder builder () {
            return new CloudFoundryPlatformInfoBuilder();
        }

        public CloudFoundryPlatformInfoBuilder setInfoResponse (Mono<GetInfoResponse> infoResponse) {
            this.infoResponse = infoResponse;
            return this;
        }

        public CloudFoundryPlatformInfoBuilder setSshCodeResponse (Mono<String> sshCodeResponse) {
            this.sshCodeResponse = sshCodeResponse;
            return this;
        }

        public CloudFoundryPlatformInfo build () {
            CloudFoundryPlatformInfo cloudFoundryPlatformInfo = new CloudFoundryPlatformInfo();
            cloudFoundryPlatformInfo.infoResponse = this.infoResponse;
            cloudFoundryPlatformInfo.sshCodeResponse = this.sshCodeResponse;
            return cloudFoundryPlatformInfo;
        }
    }
}
