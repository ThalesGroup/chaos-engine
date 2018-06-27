package com.gemalto.chaos.platform.impl;

import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;

public class CloudFoundryPlatformInfoTest {
    private final String SSHCODE = "sre33vsdv";
    private final String ENDPOINT = "ssh.endpoint.org";
    private final String PORT = "2222";
    private final String APIVERSION = "2.6.58";
    private final String FINGERPRINT = "3d:45:b3:25:fc:3b:03:8c:3c:21:a1:41:2f:a9:97:7d";
    private CloudFoundryPlatformInfo cfpi;

    @Before
    public void setUp () {
        Mono<String> sshCodeResponse = Mono.just(SSHCODE);
        GetInfoResponse infoResponse = GetInfoResponse.builder()
                                                      .apiVersion(APIVERSION)
                                                      .applicationSshEndpoint(ENDPOINT + ":" + PORT)
                                                      .applicationSshHostKeyFingerprint(FINGERPRINT)
                                                      .build();
        Mono<GetInfoResponse> getInfoResponse = Mono.just(infoResponse);
        cfpi = CloudFoundryPlatformInfo.builder()
                                       .setInfoResponse(getInfoResponse)
                                       .setSshCodeResponse(sshCodeResponse)
                                       .build();
        cfpi.fetchInfo();
    }

    @Test
    public void getApplicationSshEndpoint () {
        assertEquals(ENDPOINT, cfpi.getApplicationSshEndpoint());
    }

    @Test
    public void getSshCode () {
        assertEquals(SSHCODE, cfpi.getSshCode());
    }

    @Test
    public void getApplicationSshHostKeyFingerprint () {
        assertEquals(FINGERPRINT, cfpi.getApplicationSshHostKeyFingerprint());
    }

    @Test
    public void getApplicationSshPort () {
        assertEquals(PORT, cfpi.getApplicationSshPort());
    }

    @Test
    public void getPlatformVersion () {
        assertEquals(APIVERSION, cfpi.getPlatformVersion());
    }
}