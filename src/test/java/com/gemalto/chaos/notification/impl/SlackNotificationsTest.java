package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.util.HttpUtils;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.gemalto.chaos.notification.impl.SlackNotifications.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class SlackNotificationsTest {
    private ChaosEvent chaosEvent;
    private SlackNotifications slackNotifications;
    private String slack_webhookuri = "";
    @Mock
    private Platform platform;
    @Mock
    private Container container;
    private DataDogNotification.DataDogEvent dataDogEvent;
    private String experimentId = UUID.randomUUID().toString();
    private String experimentMethod = UUID.randomUUID().toString();
    private String message = UUID.randomUUID().toString();
    private NotificationLevel level = NotificationLevel.WARN;

    @Before
    public void setUp () throws Exception {
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
        when(container.getSimpleName()).thenReturn(UUID.randomUUID().toString());
        when(container.getPlatform()).thenReturn(platform);
        when(platform.getPlatformType()).thenReturn("TYPE");
    }

    @Test
    public void logEvent () throws IOException {
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
                                                         .withCollapsibleField(RAW_EVENT, chaosEvent.toString())
                                                         .build();
        SlackMessage.SlackMessageBuilder slackMessageBuilder = SlackMessage.builder();
        SlackMessage expectedSlackMessage = slackMessageBuilder.withAttachment(slackAttachment).build();
        ArgumentCaptor<SlackMessage> slackMessageArgumentCaptor = ArgumentCaptor.forClass(SlackMessage.class);
        doNothing().when(slackNotifications).sendSlackMessage(ArgumentMatchers.any());
        slackNotifications.logEvent(chaosEvent);
        slackNotifications.flushBuffer();
        verify(slackNotifications, times(1)).sendSlackMessage(slackMessageArgumentCaptor.capture());
        List<SlackMessage> slackMessages = slackMessageArgumentCaptor.getAllValues();
        SlackMessage actualSlackMessage = slackMessages.get(0);
        String expectedPayload = new Gson().toJson(expectedSlackMessage);
        String actualPayload = new Gson().toJson(actualSlackMessage);
        assertEquals(expectedPayload, actualPayload);
    }
}