package com.gemalto.chaos.util;

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

public class HttpUtilsTest {
    private static final String RESPONSE = "This is a test response";
    private HttpServer server;
    private Integer port;

    @Before
    public void setUp () throws Exception {
        InetSocketAddress socket = new InetSocketAddress(0);
        server = HttpServer.create(socket, 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    @Test
    public void curl () throws IOException {
        assertEquals(RESPONSE, HttpUtils.curl(new URL("http", "localhost", port, "/test").toString()));
    }

    @After
    public void tearDown () {
        server.stop(0);
    }

    private class MyHandler implements HttpHandler {
        @Override
        public void handle (HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(200, RESPONSE.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(RESPONSE.getBytes());
            os.close();
        }
    }
}