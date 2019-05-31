package com.thales.chaos.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.notification.BufferedNotificationMethod;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.util.HttpUtils;
import com.thales.chaos.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thales.chaos.exception.enums.ChaosErrorCode.NOTIFICATION_SEND_ERROR;
import static java.util.function.Predicate.not;

@Component
@ConditionalOnProperty({ "slack_webhookuri" })
public class SlackNotifications extends BufferedNotificationMethod {
    static final Integer MAXIMUM_ATTACHMENTS = 20;
    static final String TITLE = "Message";
    static final String EXPERIMENT_ID = "Experiment ID";
    static final String TARGET = "Target";
    static final String EXPERIMENT_METHOD = "Method";
    static final String EXPERIMENT_TYPE = "Type";
    static final String PLATFORM_LAYER = "Platform Layer";
    static final String RAW_EVENT = "Raw Event";
    static final String FOOTER_PREFIX = "Chaos Engine - ";
    private final EnumMap<NotificationLevel, String> slackNotificationColorMap = new EnumMap<>(NotificationLevel.class);
    private String webhookUri;
    private Queue<SlackAttachment> attachmentQueue = new ConcurrentLinkedQueue<>();
    private String hostname;
    private final Collection<String> knownChaosEventFields = List.of("title", "message", "notificationLevel", "experimentId", "experimentType", "experimentMethod", "chaosTime", "targetContainer");

    @Autowired
    SlackNotifications (@Value("${slack_webhookuri}") @NotNull String webhookUri) {
        super();
        this.webhookUri = webhookUri;
        this.hostname = HttpUtils.getMachineHostname();
        slackNotificationColorMap.put(NotificationLevel.GOOD, "good");
        slackNotificationColorMap.put(NotificationLevel.WARN, "warning");
        log.info("Created Slack notification manager against {}", getObfuscatedWebhookURI(this.webhookUri));
    }

    @Override
    public void logNotification (ChaosNotification notification) {
        attachmentQueue.offer(createAttachmentFromChaosNotification(notification));
        if (attachmentQueue.size() >= MAXIMUM_ATTACHMENTS) {
            flushBuffer();
        }
    }

    private SlackAttachment createAttachmentFromChaosNotification (ChaosNotification chaosNotification) {
        SlackAttachment.SlackAttachmentBuilder builder;
        builder = SlackAttachment.builder().withFallback(chaosNotification.asMap().toString())
                                 .withFooter(FOOTER_PREFIX + hostname)
                                 .withTitle(TITLE)
                                 .withColor(getSlackNotificationColor(chaosNotification.getNotificationLevel()))
                                 .withText(chaosNotification.getMessage())
                                 .withAuthor_name(chaosNotification.getTitle())
                                 .withPretext(chaosNotification.getNotificationLevel().toString())
                                 .withTs(Instant.now());
        if (chaosNotification instanceof ChaosExperimentEvent) {
            ChaosExperimentEvent evt = (ChaosExperimentEvent) chaosNotification;
            builder.withTs(evt.getChaosTime().toInstant())
                   .withField(EXPERIMENT_ID, evt.getExperimentId())
                   .withField(TARGET, evt.getTargetContainer().getSimpleName())
                   .withField(EXPERIMENT_METHOD, evt.getExperimentMethod())
                   .withField(EXPERIMENT_TYPE, evt.getExperimentType().toString())
                   .withField(PLATFORM_LAYER, evt.getTargetContainer().getPlatform().getPlatformType());
        }
        builder.withCodeField(RAW_EVENT, chaosNotification.toString())
                                 .withMarkupIn(SlackAttachment.MarkupOpts.fields)
                                 .withMarkupIn(SlackAttachment.MarkupOpts.pretext);
        chaosNotification.asMap()
                         .entrySet()
                         .stream()
                         .filter(not(e -> knownChaosEventFields.contains(e.getKey().toString())))
                         .forEach(e -> builder.withField(StringUtils.convertCamelCaseToSentence(e.getKey()
                                                                                          .toString()), e.getValue()
                                                                                                         .toString()));
        return builder.build();
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
            throw new ChaosException(NOTIFICATION_SEND_ERROR, e);
        }
    }

    @Override
    protected void sendNotification (ChaosNotification chaosNotification) throws IOException {
        SlackMessage slackMessage = SlackMessage.builder()
                                                .withAttachment(createAttachmentFromChaosNotification(chaosNotification))
                                                .build();
        sendSlackMessage(slackMessage);
    }


    String getSlackNotificationColor (NotificationLevel notificationLevel) {
        return Optional.ofNullable(slackNotificationColorMap.get(notificationLevel)).orElse("danger");
    }

    void sendSlackMessage (SlackMessage slackMessage) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response;
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(slackMessage), headers);
        try {
            response = restTemplate.postForEntity(webhookUri, httpEntity, String.class);
        } catch (HttpServerErrorException e) {
            throw new IOException("unexpected response from server");
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("unexpected response from server");
        }
    }

    static String getObfuscatedWebhookURI (String webhookUri) {
        String obfuscatedWebhookURI = webhookUri;
        Pattern webhookPattern = Pattern.compile("^https?://hooks.slack.com/services/T..(.*?)/B..(.*?)/...(.*?)/?$");
        Matcher matcher = webhookPattern.matcher(webhookUri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                int start = matcher.start(i);
                int end = matcher.end(i);
                String section = obfuscatedWebhookURI.substring(start, end);
                String obfuscation = new String(new char[section.length()]).replace('\0', '*');
                obfuscatedWebhookURI = obfuscatedWebhookURI.replace(section, obfuscation);
            }
        }
        if (obfuscatedWebhookURI.equals(webhookUri)) {
            return null;
        }
        return obfuscatedWebhookURI;
    }
}

class SlackMessage {
    private String text;
    private List<SlackAttachment> attachments;

    static SlackMessageBuilder builder () {
        return new SlackMessageBuilder();
    }

    public String getText () {
        return text;
    }

    public List<SlackAttachment> getAttachments () {
        return attachments;
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

    static SlackAttachmentBuilder builder () {
        return new SlackAttachmentBuilder();
    }

    public String getPretext () {
        return pretext;
    }

    public String getAuthor_name () {
        return author_name;
    }

    public String getTitle () {
        return title;
    }

    public String getTitle_link () {
        return title_link;
    }

    public String getText () {
        return text;
    }

    public String getColor () {
        return color;
    }

    public String getFooter () {
        return footer;
    }

    public Long getTs () {
        return ts;
    }

    public String getFallback () {
        return fallback;
    }

    public List<Field> getFields () {
        return fields;
    }

    public enum MarkupOpts {
        fields,
        text,
        pretext,
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

    public String getTitle () {
        return title;
    }

    public String getValue () {
        return value;
    }

    public boolean isShort () {
        return Short;
    }
}