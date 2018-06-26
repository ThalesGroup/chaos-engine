package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.notification.BufferedNotificationMethod;
import com.gemalto.chaos.notification.ChaosEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

@Component
@ConditionalOnProperty({ "slack_webhookuri" })
public class SlackNotifications extends BufferedNotificationMethod {
    private String webhookUri;
    private String slackChannel;

    @Autowired
    SlackNotifications (@Value("${slack_webhookuri}") @NotNull String webhookUri, @Value("${slack_channel:chaos}") @NotNull String slackChannel) {
        super();
        this.webhookUri = webhookUri;
        this.slackChannel = slackChannel;
    }

    @Override
    protected void sendNotification (ChaosEvent chaosEvent) throws IOException {
        URL url = new URL(webhookUri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "Chaos Engine");
        requestBody.put("channel", slackChannel);
        requestBody.put("text", chaosEvent.toString());
        OutputStream outputStream = connection.getOutputStream();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
            bufferedWriter.write("Hello, world");
        } catch (Exception e) {
            log.error("Unknown exception", e);
        }
    }
}
