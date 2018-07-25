package com.gemalto.chaos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils () {
    }

    public static String curl (String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            return connection.getResponseMessage();
        } catch (IOException e) {
            log.error("Exception when polling {}", url, e);
            return null;
        }
    }
}
