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

package com.thales.chaos.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AwsMetadataUtilTest {
    private static final String RESPONSE = "{\n" + "    \"devpayProductCodes\" : null,\n" + "    \"marketplaceProductCodes\" : [ \"1abc2defghijklm3nopqrs4tu\" ], \n" + "    \"availabilityZone\" : \"us-west-2b\",\n" + "    \"privateIp\" : \"10.158.112.84\",\n" + "    \"version\" : \"2017-09-30\",\n" + "    \"instanceId\" : \"i-1234567890abcdef0\",\n" + "    \"billingProducts\" : null,\n" + "    \"instanceType\" : \"t2.micro\",\n" + "    \"accountId\" : \"123456789012\",\n" + "    \"imageId\" : \"ami-5fb8c835\",\n" + "    \"pendingTime\" : \"2016-11-19T16:32:11Z\",\n" + "    \"architecture\" : \"x86_64\",\n" + "    \"kernelId\" : null,\n" + "    \"ramdiskId\" : null,\n" + "    \"region\" : \"us-west-2\"\n" + "}";
    private HttpServer server;
    private Integer port;
    private URI uri;

    @Before
    public void setUp () throws Exception {
        InetSocketAddress socket = new InetSocketAddress(0);
        server = HttpServer.create(socket, 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
        uri = UriComponentsBuilder.newInstance()
                                  .scheme("http")
                                  .host("localhost")
                                  .port(port)
                                  .path("/test")
                                  .build()
                                  .toUri();
    }

    @After
    public void tearDown () {
        server.stop(0);
    }

    @Test
    public void fetchAwsInstanceIdentity () {
        AwsMetadataUtil.fetchAwsInstanceIdentity(uri);
        AwsMetadataUtil.AwsInstanceIdentity identity = AwsMetadataUtil.getAwsInstanceIdentity();
        assertNotNull(identity);
        assertEquals("i-1234567890abcdef0", identity.getInstanceId());
        assertEquals("us-west-2b", identity.getAvailabilityZone());
        assertEquals("123456789012", identity.getAccountId());
        assertEquals("ami-5fb8c835", identity.getImageId());
        assertEquals("us-west-2", identity.getRegion());
    }

    private class MyHandler implements HttpHandler {
        @Override
        public void handle (HttpExchange httpExchange) throws IOException {
            httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
            httpExchange.sendResponseHeaders(200, RESPONSE.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(RESPONSE.getBytes());
            os.close();
        }
    }
}