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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpUtilsTest {
    private static final String RESPONSE = "This is a test response";
    private HttpServer server;
    private Integer port;

    @Before
    public void setUp () throws Exception {
        InetSocketAddress socket = new InetSocketAddress(0);
        server = HttpServer.create(socket, 0);
        server.createContext("/test", new MyHandler(200));
        server.createContext("/notfound", new MyHandler(404));
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    @Test
    public void curl () throws IOException {
        assertEquals(RESPONSE, HttpUtils.curl(new URL("http", "localhost", port, "/test").toString()));
    }

    @Test
    public void curlNotFound () throws IOException {
        assertNull(HttpUtils.curl(new URL("http", "localhost", port, "/notfound").toString()));
    }

    @After
    public void tearDown () {
        server.stop(0);
    }

    private class MyHandler implements HttpHandler {
        private int rCode;

        public MyHandler (int rCode) {
            this.rCode = rCode;
        }

        @Override
        public void handle (HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(rCode, RESPONSE.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(RESPONSE.getBytes());
            os.close();
        }
    }
}