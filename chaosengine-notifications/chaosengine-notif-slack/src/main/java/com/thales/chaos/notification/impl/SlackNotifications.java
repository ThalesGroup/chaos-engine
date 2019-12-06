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

package com.thales.chaos.notification.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.notification.BufferedNotificationMethod;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.enums.NotificationLevel;
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
    static final String FOOTER_PREFIX = "Chaos Engine - ";
    private final EnumMap<NotificationLevel, String> slackNotificationColorMap = new EnumMap<>(NotificationLevel.class);
    private String webhookUri;
    private Queue<SlackAttachment> attachmentQueue = new ConcurrentLinkedQueue<>();
    private String hostname;
    private final Collection<String> knownChaosEventFields = List.of("title", "message", "notificationLevel", "experimentId", "experimentType", "experimentMethod", "chaosTime", "targetContainer");
    private final Collection<String> knownContainerFields = List.of("aggregationIdentifier", "simpleName", "containerType");

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
        Map<String, Object> fieldMap = chaosNotification.asMap();
        builder = SlackAttachment.builder().withFallback(fieldMap.toString())
                                 .withFooter(FOOTER_PREFIX + hostname)
                                 .withTitle(TITLE)
                                 .withColor(getSlackNotificationColor(chaosNotification.getNotificationLevel()))
                                 .withText(chaosNotification.getMessage()).withAuthorName(chaosNotification.getTitle())
                                 .withPretext(chaosNotification.getNotificationLevel().toString())
                                 .withTs(Instant.now());
        collectExperimentEventFields(fieldMap, builder);
        chaosNotification.asMap()
                         .entrySet()
                         .stream()
                         .filter(not(e -> knownChaosEventFields.contains(e.getKey())))
                         .forEach(e -> builder.withField(StringUtils.convertCamelCaseToSentence(e.getKey()), e.getValue()
                                                                                                              .toString()));
        return builder.build();
    }

    private void collectExperimentEventFields (Map<String, Object> fieldMap, SlackAttachment.SlackAttachmentBuilder builder) {
        fieldMap.entrySet()
                .stream()
                .filter(e -> knownChaosEventFields.contains(e.getKey()))
                .filter(e -> e.getKey().startsWith("experiment") && e.getValue() != null)
                .forEach(e -> builder.withField(StringUtils.convertCamelCaseToSentence(e.getKey()), e.getValue().toString()));
        Optional.ofNullable(fieldMap.get("chaosTime"))
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .map(Instant::ofEpochMilli)
                .ifPresent(builder::withTs);
        Optional.ofNullable(fieldMap.get("targetContainer"))
                .filter(Map.class::isInstance)
                .map(o -> (Map<String, String>) o)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .filter(entry -> knownContainerFields.contains(entry.getKey()))
                .forEach(e -> builder.withField(StringUtils.convertCamelCaseToSentence(e.getKey()), e.getValue()));
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
            log.debug("Sending Slack notification");
            sendSlackMessage(slackMessage);
            log.debug("Slack notification sent");
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
            throw new IOException("unexpected response from server: " + e.getMessage());
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("unexpected response from server: HTTP status code " + response.getStatusCode());
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
    @JsonProperty("text")
    private String text;
    @JsonProperty("attachments")
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
        // Message format definition: https://api.slack.com/docs/messages/builder
class SlackAttachment {
    @JsonProperty("pretext")
    private String pretext;
    @JsonProperty("author_name")
    private String authorName;
    @JsonProperty("title")
    private String title;
    @JsonProperty("title_link")
    private String titleLink;
    @JsonProperty("text")
    private String text;
    @JsonProperty("color")
    private String color;
    @JsonProperty("footer")
    private String footer;
    @JsonProperty("ts")
    private Long ts;
    @JsonProperty("fallback")
    private String fallback;
    @JsonProperty("fields")
    private List<Field> fields;

    static SlackAttachmentBuilder builder () {
        return new SlackAttachmentBuilder();
    }

    public String getPretext () {
        return pretext;
    }

    public String getAuthorName () {
        return authorName;
    }

    public String getTitle () {
        return title;
    }

    public String getTitleLink () {
        return titleLink;
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
        FIELDS,
        TEXT,
        PRETEXT,
    }

    public static final class SlackAttachmentBuilder {
        private String pretext;
        private String authorName;
        private String title;
        private String titleLink;
        private String text;
        private String color;
        private String footer;
        private Long ts;
        private String fallback;
        private List<MarkupOpts> mrkdwnIn = new ArrayList<>();
        private List<Field> fields = new ArrayList<>();

        private SlackAttachmentBuilder () {
        }

        public SlackAttachmentBuilder withPretext (String pretext) {
            String pretextPrefix = "_*";
            String pretextSuffix = "*_";
            this.pretext = pretextPrefix + pretext + pretextSuffix;
            return this;
        }

        public SlackAttachmentBuilder withAuthorName (String authorName) {
            this.authorName = authorName;
            return this;
        }

        SlackAttachmentBuilder withTitle (String title) {
            this.title = title;
            return this;
        }

        SlackAttachmentBuilder withTitleLink (String titleLink) {
            this.titleLink = titleLink;
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
            this.mrkdwnIn.add(value);
            return this;
        }

        SlackAttachment build () {
            SlackAttachment slackAttachment = new SlackAttachment();
            slackAttachment.pretext = this.pretext;
            slackAttachment.authorName = this.authorName;
            slackAttachment.color = this.color;
            slackAttachment.title = this.title;
            slackAttachment.text = this.text;
            slackAttachment.titleLink = this.titleLink;
            slackAttachment.fallback = this.fallback;
            slackAttachment.footer = this.footer;
            slackAttachment.ts = this.ts;
            slackAttachment.fields = this.fields;
            return slackAttachment;
        }
    }
}

class Field {
    @JsonProperty("title")
    String title;
    @JsonProperty("value")
    String value;
    @JsonProperty("short")
    boolean isShort = false;

    public Field (String title, String value) {
        this.title = title;
        this.value = value;
    }

    public Field (String title, String value, boolean isShort) {
        this.title = title;
        this.value = value;
        this.isShort = isShort;
    }

    public String getTitle () {
        return title;
    }

    public String getValue () {
        return value;
    }

    public boolean isShort () {
        return isShort;
    }
}