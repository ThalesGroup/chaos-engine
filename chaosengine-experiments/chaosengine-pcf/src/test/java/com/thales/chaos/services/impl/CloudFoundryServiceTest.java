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

import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
public class CloudFoundryServiceTest {
    private CloudFoundryService cloudFoundryService = spy(new CloudFoundryService());
    private String apiHost = "my-pcf.example.com";
    private Integer port = 8443;
    private String organization = "my-pcf-organization";
    private String username = "my-pcf-username";
    private String password = "my-pcf-password";
    private String space = "my-pcf-space";

    @Before
    public void setUp () {
        cloudFoundryService.setApiHost(apiHost);
        cloudFoundryService.setPort(port);
        cloudFoundryService.setOrganization(organization);
        cloudFoundryService.setUsername(username);
        cloudFoundryService.setPassword(password);
        cloudFoundryService.setSpace(space);
    }

    @Test
    public void connectionContext () {
        DefaultConnectionContext connectionContext = (DefaultConnectionContext) cloudFoundryService.connectionContext();
        testDefaultConnectionContext(connectionContext);
    }

    private void testDefaultConnectionContext (DefaultConnectionContext connectionContext) {
        assertEquals(apiHost, connectionContext.getApiHost());
        assertEquals(port, connectionContext.getPort().orElseThrow());
    }

    @Test
    public void proxyConfiguration () {
        Properties oldProperties = System.getProperties();
        Optional<ProxyConfiguration> proxyConfiguration;
        try {
            System.setProperty("https.proxyHost", "my-pcf-proxy");
            System.setProperty("https.proxyPort", "12345");
            proxyConfiguration = cloudFoundryService.proxyConfiguration();
        } finally {
            System.setProperties(oldProperties);
        }
        assertTrue(proxyConfiguration.isPresent());
        assertEquals("my-pcf-proxy", proxyConfiguration.get().getHost());
        assertEquals((Integer) 12345, proxyConfiguration.get().getPort().orElseThrow());
    }

    @Test
    public void noProxyConfiguration () {
        Properties oldProperties = System.getProperties();
        Optional<ProxyConfiguration> proxyConfiguration;
        try {
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            proxyConfiguration = cloudFoundryService.proxyConfiguration();
        } finally {
            System.setProperties(oldProperties);
        }
        assertTrue(proxyConfiguration.isEmpty());
    }

    @Test
    public void tokenProvider () {
        PasswordGrantTokenProvider tokenProvider = (PasswordGrantTokenProvider) cloudFoundryService.tokenProvider();
        assertEquals(password, tokenProvider.getPassword());
        assertEquals(username, tokenProvider.getUsername());
    }
}