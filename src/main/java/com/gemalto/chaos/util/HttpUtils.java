package com.gemalto.chaos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils () {
    }

    public static String curl (String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            InputStream response = connection.getInputStream();
            return new BufferedReader(new InputStreamReader(response)).lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            log.error("Exception when polling {}", url, e);
            return null;
        }
    }
}
