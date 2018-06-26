package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.platform.PlatformInfo;
import com.gemalto.chaos.services.impl.CloudFoundryService;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
@ConditionalOnBean(CloudFoundryService.class)
public class CloudFoundryPlatformInfo implements PlatformInfo {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundryPlatformInfo.class);
    @Autowired
    private ReactorCloudFoundryClient cloudFoundryClient;
    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;
    private GetInfoResponse info;

    public CloudFoundryPlatformInfo (ReactorCloudFoundryClient cloudFoundryClient, CloudFoundryOperations cloudFoundryOperations) {
        this.cloudFoundryClient = cloudFoundryClient;
        this.cloudFoundryOperations = cloudFoundryOperations;
        log.info("Gathering platform info.");
    }

    @Override
    public String platformVersion () {
        return getInfo().getApiVersion();
    }

    private GetInfoResponse getInfo () {
        if (info == null) {
            info = cloudFoundryClient.info().get(getInfoRequest()).block();
        }
        return info;
    }

    private GetInfoRequest getInfoRequest () {
        return GetInfoRequest.builder().build();
    }

    public String applicationSshEndpoint () {
        String endpoint = getInfo().getApplicationSshEndpoint();
        try {
            URL url = new URL(endpoint);
            return url.getHost();
        } catch (MalformedURLException e) {
            log.error("Cannot get application SSH endpoint {}", e);
        }
        return null;
    }

    public int applicationSshPort () {
        String endpoint = getInfo().getApplicationSshEndpoint();
        try {
            URL url = new URL(endpoint);
            return url.getPort();
        } catch (MalformedURLException e) {
            log.error("Cannot get application SSH port {}", e);
        }
        return 0;
    }

    public String getSshCode () {
        return cloudFoundryOperations.advanced().sshCode().block();
    }

    public String applicationSshHostKeyFingerprint () {
        return getInfo().getApplicationSshHostKeyFingerprint();
    }
}
