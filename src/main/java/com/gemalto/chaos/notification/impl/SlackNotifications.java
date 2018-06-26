package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.notification.BufferedNotificationMethod;
import com.gemalto.chaos.notification.ChaosEvent;
import com.google.gson.Gson;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty({ "slack_webhookuri" })
public class SlackNotifications extends BufferedNotificationMethod {
    private String webhookUri;

    @Autowired
    SlackNotifications (@Value("${slack_webhookuri}") @NotNull String webhookUri) {
        super();
        this.webhookUri = webhookUri;
        log.info("Created Slack notification manager against " + this.webhookUri);
    }

    @Override
    protected void sendNotification (ChaosEvent chaosEvent) throws IOException {
        SlackMessage slackMessage = SlackMessage.builder()
                                                .withAttachment(SlackAttachment.builder()
                                                                               .withFallback(chaosEvent.toString())
                                                                               .withFooter("Chaos Engine")
                                                                               .withTitle("Chaos Event against " + chaosEvent
                                                                                       .getTargetContainer()
                                                                                       .getSimpleName())
                                                                               .withColor("good")
                                                                               .withText(chaosEvent.toString())
                                                                               .withTs(chaosEvent.getChaosTime()
                                                                                                 .toInstant())
                                                                               .build())
                                                .build();
        String payload = new Gson().toJson(slackMessage);
        try {
            URL url = new URL(webhookUri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
                bufferedWriter.write(payload);
                bufferedWriter.flush();
            } catch (Exception e) {
                log.error("Unknown exception sending payload " + payload, e);
            }
        } catch (IOException e) {
            log.debug("Failed to send payload: " + payload, e);
            throw e;
        }
    }
}

@SuppressWarnings("unused")
        // Variables are used as part of Gson, despite appearing unused.
class SlackMessage {
    private String text;
    private List<SlackAttachment> attachments;

    static SlackMessageBuilder builder () {
        return new SlackMessageBuilder();
    }

    public static final class SlackMessageBuilder {
        private String text;
        private List<SlackAttachment> attachments;

        private SlackMessageBuilder () {
        }

        SlackMessageBuilder withText (String text) {
            this.text = text;
            return this;
        }

        SlackMessageBuilder withAttachment (SlackAttachment attachment) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(attachment);
            return this;
        }

        SlackMessage build () {
            SlackMessage slackMessage = new SlackMessage();
            slackMessage.text = this.text;
            slackMessage.attachments = this.attachments;
            return slackMessage;
        }
    }
}

@SuppressWarnings("unused")
        // Variables are used as part of Gson, despite appearing unused.
class SlackAttachment {
    private String title;
    private String title_link;
    private String text;
    private String color;
    private String footer;
    private Long ts;
    private String fallback;

    static SlackAttachmentBuilder builder () {
        return new SlackAttachmentBuilder();
    }

    public static final class SlackAttachmentBuilder {
        private String title;
        private String title_link;
        private String text;
        private String color;
        private String footer;
        private Long ts;
        private String fallback;

        private SlackAttachmentBuilder () {
        }

        SlackAttachmentBuilder withTitle (String title) {
            this.title = title;
            return this;
        }

        SlackAttachmentBuilder withTitle_link (String title_link) {
            this.title_link = title_link;
            return this;
        }

        SlackAttachmentBuilder withText (String text) {
            this.text = text;
            return this;
        }

        SlackAttachmentBuilder withColor (String color) {
            this.color = color;
            return this;
        }

        SlackAttachmentBuilder withFooter (String footer) {
            this.footer = footer;
            return this;
        }

        SlackAttachmentBuilder withTs (Instant timestamp) {
            this.ts = timestamp.getEpochSecond();
            return this;
        }

        SlackAttachmentBuilder withFallback (String fallback) {
            this.fallback = fallback;
            return this;
        }

        SlackAttachment build () {
            SlackAttachment slackAttachment = new SlackAttachment();
            slackAttachment.color = this.color;
            slackAttachment.title = this.title;
            slackAttachment.text = this.text;
            slackAttachment.title_link = this.title_link;
            slackAttachment.fallback = this.fallback;
            slackAttachment.footer = this.footer;
            slackAttachment.ts = this.ts;
            return slackAttachment;
        }
    }
}

