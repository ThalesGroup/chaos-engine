/*
 *    Copyright (c) 2019 Thales Group
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static final HttpClient client;

    static {
        client = HttpClient.newBuilder().build();
    }

    private HttpUtils () {
    }

    public static String getMachineHostname () {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Cannot resolve machine hostname", e);
            hostname=getMachineIP();
        }
        return hostname;
    }

    public static String getMachineIP () {
        String ip = "";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Cannot retrieve machine IP", e);
        }
        return ip;
    }

    static String curl (String url) {
        return curl(url, false);
    }

    public static String curl (String url, boolean suppressErrors) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (!isGoodResponse(statusCode)) {
                throw new IOException("Received HTTP " + statusCode + " response.");
            }
            return response.body();
        } catch (IOException e) {
            if (suppressErrors) return null;
            log.error("Exception when polling {}", url, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted when polling {}", url, e);
            return null;
        }
    }

    private static boolean isGoodResponse (int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
