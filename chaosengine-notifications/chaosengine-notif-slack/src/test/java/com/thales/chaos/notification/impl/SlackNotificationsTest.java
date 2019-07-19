package com.thales.chaos.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.notification.message.ChaosMessage;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.util.HttpUtils;
import com.thales.chaos.util.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class SlackNotificationsTest {
    private static final String OK_RESPONSE = "ok";
    private ChaosExperimentEvent chaosExperimentEvent;
    private ChaosMessage chaosMessage;
    private SlackNotifications slackNotifications;
    private SlackMessage expectedSlackEvent;
    private SlackMessage expectedSlackMessage;
    private static final String EXPERIMENT_ID = "Experiment Id";
    private static final String EXPERIMENT_TYPE = "Experiment Type";
    private static final String EXPERIMENT_METHOD = "Experiment Method";
    @Mock
    private Platform platform;
    private static final String AGGREGATION_IDENTIFIER = "Aggregation Identifier";
    private String experimentId = UUID.randomUUID().toString();
    private String experimentMethod = UUID.randomUUID().toString();
    private String message = StringUtils.generateRandomString(50);
    private String title = StringUtils.generateRandomString(50);
    private NotificationLevel level = NotificationLevel.WARN;
    private static final String SIMPLE_NAME = "Simple Name";
    private String slack_webhookuri;
    private HttpServer slackServerMock;
    private Integer slackServerPort;
    private int responseCode = 200;
    private ObjectMapper mapper = new ObjectMapper();
    private static final String CONTAINER_TYPE = "Container Type";
    private String aggregationIdentifier = UUID.randomUUID().toString();
    private String simpleName = UUID.randomUUID().toString();
    private String containerType = UUID.randomUUID().toString();
    private Container container = new Container() {
        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return null;
        }

        @Override
        public String getSimpleName () {
            return simpleName;
        }

        @Override
        public String getAggregationIdentifier () {
            return aggregationIdentifier;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }

        @Override
        public String getContainerType () {
            return containerType;
        }
    };
    private Date chaosTime = Date.from(Instant.now());
    private ExperimentType experimentType = ExperimentType.STATE;

    @After
    public void tearDown () {
        slackServerMock.stop(0);
    }

    @Before
    public void setUp () throws Exception {
        setupMockServer();
        slackNotifications = Mockito.spy(new SlackNotifications(slack_webhookuri));
        chaosExperimentEvent = ChaosExperimentEvent.builder()
                                                   .withMessage(message)
                                                   .withExperimentId(experimentId)
                                                   .withMessage(message)
                                                   .withNotificationLevel(level)
                                                   .withTargetContainer(container)
                                                   .withExperimentMethod(experimentMethod)
                                                   .withExperimentType(experimentType)
                                                   .withChaosTime(chaosTime)
                                                   .build();
        when(platform.getPlatformType()).thenReturn("TYPE");
        SlackAttachment slackAttachmentEvent = SlackAttachment.builder()
                                                              .withFallback(chaosExperimentEvent.asMap().toString())
                                                              .withFooter(SlackNotifications.FOOTER_PREFIX + HttpUtils.getMachineHostname())
                                                              .withTitle(SlackNotifications.TITLE)
                                                              .withColor(slackNotifications.getSlackNotificationColor(chaosExperimentEvent
                                                                 .getNotificationLevel()))
                                                              .withText(chaosExperimentEvent.getMessage())
                                                              .withTs(chaosTime.toInstant())
                                                              .withAuthorName(chaosExperimentEvent.getTitle())
                                                              .withPretext(chaosExperimentEvent.getNotificationLevel()
                                                                                          .toString())
                                                              .withField(EXPERIMENT_ID, experimentId)
                                                              .withField(EXPERIMENT_METHOD, experimentMethod)
                                                              .withField(EXPERIMENT_TYPE, experimentType.name())
                                                              .withField(AGGREGATION_IDENTIFIER, aggregationIdentifier)
                                                              .withField(CONTAINER_TYPE, containerType)
                                                              .withField(SIMPLE_NAME, simpleName)
                                                              .build();
        SlackMessage.SlackMessageBuilder slackMessageBuilder = SlackMessage.builder();
        expectedSlackEvent = slackMessageBuilder.withAttachment(slackAttachmentEvent).build();
        slackMessageBuilder = SlackMessage.builder();
        chaosMessage = ChaosMessage.builder()
                                   .withMessage(message)
                                   .withTitle(title)
                                   .withNotificationLevel(level)
                                   .build();
        SlackAttachment slackAttachmentMessage = SlackAttachment.builder().withFallback(chaosMessage.asMap().toString())
                                                                .withFooter(SlackNotifications.FOOTER_PREFIX + HttpUtils
                                                                        .getMachineHostname())
                                                                .withTitle(SlackNotifications.TITLE)
                                                                .withColor(slackNotifications.getSlackNotificationColor(chaosMessage
                                                                        .getNotificationLevel()))
                                                                .withText(chaosMessage.getMessage())
                                                                .withTs(Instant.now())
                                                                .withAuthorName(chaosMessage.getTitle())
                                                                .withPretext(chaosMessage.getNotificationLevel()
                                                                                         .toString())
                                                                .build();
        expectedSlackMessage = slackMessageBuilder.withAttachment(slackAttachmentMessage).build();
    }

    private void setupMockServer () throws IOException {
        InetSocketAddress socket = new InetSocketAddress(0);
        slackServerMock = HttpServer.create(socket, 0);
        slackServerMock.createContext("/", new SlackNotificationsTest.SlackHandler());
        slackServerMock.setExecutor(null);
        slackServerMock.start();
        slackServerPort = slackServerMock.getAddress().getPort();
        slack_webhookuri = "http://localhost:" + slackServerPort;
    }

    @Test(expected = IOException.class)
    public void slackIOExceptionTest () throws IOException {
        try {
            responseCode = 500;
            slackNotifications.sendNotification(chaosExperimentEvent);
        } finally {
            responseCode = 200;
        }
    }



    @Test
    public void logEvent () throws IOException {
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        slackNotifications.logNotification(chaosExperimentEvent);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        SlackMessage actualSlackMessage = slackMessageArgumentCaptor.getValue();
        String expectedPayload = mapper.writeValueAsString(expectedSlackEvent);
        String actualPayload = mapper.writeValueAsString(actualSlackMessage);
        assertEquals(expectedPayload, actualPayload);
    }

    @Test
    public void logMessage () throws IOException {
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        slackNotifications.logNotification(chaosMessage);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        SlackMessage actualSlackMessage = slackMessageArgumentCaptor.getValue();
        String expectedPayload = mapper.writeValueAsString(expectedSlackMessage);
        String actualPayload = mapper.writeValueAsString(actualSlackMessage);
        assertEquals(expectedPayload, actualPayload);
    }

    @Test
    public void sendNotification () throws IOException {
        slackNotifications.sendNotification(chaosExperimentEvent);
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        SlackMessage actualSlackMessage = slackMessageArgumentCaptor.getValue();
        String expectedPayload = mapper.writeValueAsString(expectedSlackEvent);
        String actualPayload = mapper.writeValueAsString(actualSlackMessage);
        assertEquals(expectedPayload, actualPayload);
    }

    @Test
    public void getSlackNotificationColor () {
        assertEquals("danger", slackNotifications.getSlackNotificationColor(NotificationLevel.ERROR));
        assertEquals("warning", slackNotifications.getSlackNotificationColor(NotificationLevel.WARN));
        assertEquals("good", slackNotifications.getSlackNotificationColor(NotificationLevel.GOOD));
    }

    @Test
    public void bufferFlush () {
        for (int i = 0; i < SlackNotifications.MAXIMUM_ATTACHMENTS; i++) {
            slackNotifications.logNotification(chaosExperimentEvent);
        }
        verify(slackNotifications, times(1)).flushBuffer();
    }

    @Test
    public void ExtendedChaosEventTest () throws IOException {
        ChaosExperimentEvent x = new ChaosExperimentEvent() {
            public String getNewField () {
                return "12345";
            }

            @Override
            public NotificationLevel getNotificationLevel () {
                return NotificationLevel.GOOD;
            }

            @Override
            public Container getTargetContainer () {
                return container;
            }

            @Override
            public String getTitle () {
                return StringUtils.generateRandomString(60);
            }
        };
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        slackNotifications.logNotification(x);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        SlackMessage actualSlackMessage = slackMessageArgumentCaptor.getValue();
        assertTrue(actualSlackMessage.getAttachments()
                                     .stream()
                                     .anyMatch(slackAttachment -> slackAttachment.getFields()
                                                                                 .stream()
                                                                                 .anyMatch(field -> field.getTitle()
                                                                                                         .equals("New Field") && field
                                                                                         .getValue()
                                                                                         .equals("12345"))));
    }

    @Test
    public void getObfuscatedWebhookURI () {
        assertEquals("https://hooks.slack.com/services/T0A******/B01******/0a1*********************", SlackNotifications
                .getObfuscatedWebhookURI("https://hooks.slack.com/services/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"));
    }

    private class SlackHandler implements HttpHandler {
        @Override
        public void handle (HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(responseCode, OK_RESPONSE.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(OK_RESPONSE.getBytes());
            os.close();
        }
    }
}
