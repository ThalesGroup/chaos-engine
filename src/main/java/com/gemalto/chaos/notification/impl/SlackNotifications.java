package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.notification.BufferedNotificationMethod;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.util.HttpUtils;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.gemalto.chaos.constants.DataDogConstants.SLACK_NOTIFICATION_SERVER_RESPONSE_KEY;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@ConditionalOnProperty({ "slack_webhookuri" })
public class SlackNotifications extends BufferedNotificationMethod {
    protected static final Integer MAXIMUM_ATTACHMENTS = 20;
    private String webhookUri;
    private Queue<SlackAttachment> attachmentQueue = new ConcurrentLinkedQueue<>();
    private String hostname;

    protected static final String TITLE = "Message";
    protected static final String AUTHOR_NAME = "Chaos Event Trace";
    protected static final String EXPERIMENT_ID = "Experiment ID";
    protected static final String TARGET = "Target";
    protected static final String EXPERIMENT_METHOD = "Method";
    protected static final String EXPERIMENT_TYPE = "Type";
    protected static final String PLATFORM_LAYER = "Platform Layer";
    protected static final String RAW_EVENT = "Raw Event";
    protected static final String FOOTER_PREFIX = "Chaos Engine - ";

    @Autowired
    SlackNotifications (@Value("${slack_webhookuri}") @NotNull String webhookUri) {
        super();
        this.webhookUri = webhookUri;
        this.hostname = HttpUtils.getMachineHostname();
        log.info("Created Slack notification manager against {}", this.webhookUri);
    }

    @Override
    public void logEvent (ChaosEvent event) {
        attachmentQueue.offer(createAttachmentFromChaosEvent(event));
        if (attachmentQueue.size() >= MAXIMUM_ATTACHMENTS) {
            flushBuffer();
        }
    }

    @Override
    protected void flushBuffer () {
        if (attachmentQueue.isEmpty()) return;
        SlackMessage.SlackMessageBuilder slackMessageBuilder = SlackMessage.builder();
        SlackAttachment attachment;
        while ((attachment = attachmentQueue.poll()) != null) {
            slackMessageBuilder.withAttachment(attachment);
        }
        SlackMessage slackMessage = slackMessageBuilder.build();
        try {
            sendSlackMessage(slackMessage);
        } catch (Exception e) {
            throw new ChaosException(e);
        }
    }

    @Override
    protected void sendNotification (ChaosEvent chaosEvent) throws IOException {
        SlackMessage slackMessage = SlackMessage.builder()
                                                .withAttachment(createAttachmentFromChaosEvent(chaosEvent))
                                                .build();
        sendSlackMessage(slackMessage);
    }

    private SlackAttachment createAttachmentFromChaosEvent (ChaosEvent chaosEvent) {
        return SlackAttachment.builder()
                              .withFallback(chaosEvent.toString())
                              .withFooter(FOOTER_PREFIX + hostname)
                              .withTitle(TITLE)
                              .withColor(getSlackNotificationColor(chaosEvent.getNotificationLevel()))
                              .withText(chaosEvent.getMessage())
                              .withTs(chaosEvent.getChaosTime().toInstant())
                              .withAuthor_name(AUTHOR_NAME)
                              .withPretext(chaosEvent.getNotificationLevel().toString())
                              .withField(EXPERIMENT_ID, chaosEvent.getExperimentId())
                              .withField(TARGET, chaosEvent.getTargetContainer().getSimpleName())
                              .withField(EXPERIMENT_METHOD, chaosEvent.getExperimentMethod())
                              .withField(EXPERIMENT_TYPE, chaosEvent.getExperimentType().toString())
                              .withField(PLATFORM_LAYER, chaosEvent.getTargetContainer().getPlatform().getPlatformType())
                              .withCodeField(RAW_EVENT, chaosEvent.toString())
                              .withMarkupIn(SlackAttachment.MarkupOpts.fields)
                              .withMarkupIn(SlackAttachment.MarkupOpts.pretext)
                              .build();
    }

    protected String getSlackNotificationColor (NotificationLevel notificationLevel) {
        switch (notificationLevel) {
            case GOOD:
                return "good";
            case WARN:
                return "warning";
            default:
                return "danger";
        }
    }

    protected void sendSlackMessage (SlackMessage slackMessage) throws IOException {
        String payload = new Gson().toJson(slackMessage);
        try {
            log.debug("Sending slack notification");
            URL url = new URL(webhookUri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
                bufferedWriter.write(payload);
                bufferedWriter.flush();
            } catch (Exception e) {
                log.error("Unknown exception sending payload " + payload, e);
            }
            BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            log.debug("Slack notification status: {}", keyValue(SLACK_NOTIFICATION_SERVER_RESPONSE_KEY, response.readLine()));
            if (connection.getResponseCode() > 299 || connection.getResponseCode() < 200) {
                throw new IOException("Unexpected response from server");
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

    public List<SlackAttachment> getAttachments () {
        return attachments;
    }

    static SlackMessageBuilder builder () {
        return new SlackMessageBuilder();
    }

    static final class SlackMessageBuilder {
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
        // Message format definition: https://api.slack.com/docs/messages/builder?msg=%7B%22attachments
class SlackAttachment {
    private String pretext;
    private String author_name;
    private String title;
    private String title_link;
    private String text;
    private String color;
    private String footer;
    private Long ts;
    private String fallback;
    private List<Field> fields;

    public enum MarkupOpts {
        fields,
        text,
        pretext,
    }

    static SlackAttachmentBuilder builder () {
        return new SlackAttachmentBuilder();
    }

    public static final class SlackAttachmentBuilder {
        private String pretext;
        private String author_name;
        private String title;
        private String title_link;
        private String text;
        private String color;
        private String footer;
        private Long ts;
        private String fallback;

        private List<MarkupOpts> mrkdwn_in = new ArrayList<>();
        private List<Field> fields = new ArrayList<>();

        private SlackAttachmentBuilder () {
        }

        public SlackAttachmentBuilder withPretext (String pretext) {
            String pretext_prefix="_*";
            String pretext_suffix="*_";
            this.pretext = pretext_prefix+pretext+pretext_suffix;
            return this;
        }

        public SlackAttachmentBuilder withAuthor_name (String author_name) {
            this.author_name = author_name;
            return this;
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

        SlackAttachmentBuilder withField (String title, String value) {
            this.fields.add(new Field(title, value));
            return this;
        }

        SlackAttachmentBuilder withCodeField (String title, String value) {
            String codeMarkup="```";
            this.fields.add(new Field(title, codeMarkup+value+codeMarkup,true));
            return this;
        }

        SlackAttachmentBuilder withMarkupIn (MarkupOpts value) {
            this.mrkdwn_in.add(value);
            return this;
        }


        SlackAttachment build () {
            SlackAttachment slackAttachment = new SlackAttachment();
            slackAttachment.pretext = this.pretext;
            slackAttachment.author_name = this.author_name;
            slackAttachment.color = this.color;
            slackAttachment.title = this.title;
            slackAttachment.text = this.text;
            slackAttachment.title_link = this.title_link;
            slackAttachment.fallback = this.fallback;
            slackAttachment.footer = this.footer;
            slackAttachment.ts = this.ts;
            slackAttachment.fields = this.fields;
            return slackAttachment;
        }
    }
}

class Field {
    String title;
    String value;
    boolean Short = false;

    public Field (String title, String value) {
        this.title = title;
        this.value = value;
    }

    public Field (String title, String value, boolean Short) {
        this.title = title;
        this.value = value;
        this.Short = Short;
    }
}