package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.impl.AwsEC2Platform;
import com.gemalto.chaos.util.HttpUtils;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.gemalto.chaos.notification.impl.SlackNotifications.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class SlackNotificationsTest {
    private ChaosEvent chaosEvent;
    private SlackNotifications slackNotifications;
    private SlackMessage expectedSlackMessage;
    private static final String OK_RESPONSE = "ok";
    @Mock
    private AwsEC2Platform platform;
    @Mock
    private Container container;
    private String experimentId = UUID.randomUUID().toString();
    private String experimentMethod = UUID.randomUUID().toString();
    private String message = UUID.randomUUID().toString();
    private NotificationLevel level = NotificationLevel.WARN;
    private String slack_webhookuri;
    private HttpServer slackServerMock;
    private Integer slackServerPort;

    @After
    public void tearDown () {
        slackServerMock.stop(0);
    }

    @Before
    public void setUp () throws Exception {
        setupMockServer();
        container = AwsEC2Container.builder()
                                   .name(UUID.randomUUID().toString())
                                   .awsEC2Platform(platform)
                                   .instanceId(UUID.randomUUID().toString())
                                   .build();
        slackNotifications = Mockito.spy(new SlackNotifications(slack_webhookuri));
        chaosEvent = ChaosEvent.builder()
                               .withMessage(message)
                               .withExperimentId(experimentId)
                               .withMessage(message)
                               .withNotificationLevel(level)
                               .withTargetContainer(container)
                               .withExperimentMethod(experimentMethod)
                               .withExperimentType(ExperimentType.STATE)
                               .withChaosTime(Date.from(Instant.now()))
                               .build();
        when(platform.getPlatformType()).thenReturn("TYPE");
        SlackAttachment slackAttachment = SlackAttachment.builder()
                                                         .withFallback(chaosEvent.toString())
                                                         .withFooter(FOOTER_PREFIX + HttpUtils.getMachineHostname())
                                                         .withTitle(TITLE)
                                                         .withColor(slackNotifications.getSlackNotificationColor(chaosEvent
                                                                 .getNotificationLevel()))
                                                         .withText(chaosEvent.getMessage())
                                                         .withTs(chaosEvent.getChaosTime().toInstant())
                                                         .withAuthor_name(AUTHOR_NAME)
                                                         .withPretext(chaosEvent.getNotificationLevel().toString())
                                                         .withField(EXPERIMENT_ID, chaosEvent.getExperimentId())
                                                         .withField(TARGET, chaosEvent.getTargetContainer()
                                                                                      .getSimpleName())
                                                         .withField(EXPERIMENT_METHOD, chaosEvent.getExperimentMethod())
                                                         .withField(EXPERIMENT_TYPE, chaosEvent.getExperimentType()
                                                                                               .toString())
                                                         .withField(PLATFORM_LAYER, chaosEvent.getTargetContainer()
                                                                                              .getPlatform()
                                                                                              .getPlatformType())
                                                         .withCodeField(RAW_EVENT, chaosEvent.toString())
                                                         .build();
        SlackMessage.SlackMessageBuilder slackMessageBuilder = SlackMessage.builder();
        expectedSlackMessage = slackMessageBuilder.withAttachment(slackAttachment).build();
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

    private class SlackHandler implements HttpHandler {
        @Override
        public void handle (HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(200, OK_RESPONSE.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(OK_RESPONSE.getBytes());
            os.close();
        }
    }



    @Test
    public void logEvent () throws IOException {

        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        slackNotifications.logEvent(chaosEvent);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        List<SlackMessage> slackMessages = slackMessageArgumentCaptor.getAllValues();
        SlackMessage actualSlackMessage = slackMessages.get(0);
        String expectedPayload = new Gson().toJson(expectedSlackMessage);
        String actualPayload = new Gson().toJson(actualSlackMessage);
        assertEquals(expectedPayload, actualPayload);
    }

    @Test
    public void sendNotification () throws IOException {
        slackNotifications.sendNotification(chaosEvent);
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        List<SlackMessage> slackMessages = slackMessageArgumentCaptor.getAllValues();
        SlackMessage actualSlackMessage = slackMessages.get(0);
        String expectedPayload = new Gson().toJson(expectedSlackMessage);
        String actualPayload = new Gson().toJson(actualSlackMessage);
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
            slackNotifications.logEvent(chaosEvent);
        }
        verify(slackNotifications, times(1)).flushBuffer();
    }

    @Test
    public void ExtendedChaosEventTest () throws IOException {
        ChaosEvent x = new ChaosEvent() {
            public String getNewField () {
                return "12345";
            }

            @Override
            public ExperimentType getExperimentType () {
                return ExperimentType.STATE;
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
            public Date getChaosTime () {
                return Date.from(Instant.now());
            }
        };
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        slackNotifications.logEvent(x);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        List<SlackMessage> slackMessages = slackMessageArgumentCaptor.getAllValues();
        SlackMessage actualSlackMessage = slackMessages.get(0);
        assertTrue(actualSlackMessage.getAttachments()
                                     .stream()
                                     .anyMatch(slackAttachment -> slackAttachment.getFields()
                                                                                 .stream()
                                                                                 .anyMatch(field -> field.getTitle()
                                                                                                         .equals("New Field") && field
                                                                                         .getValue()
                                                                                         .equals("12345"))));
    }
}
