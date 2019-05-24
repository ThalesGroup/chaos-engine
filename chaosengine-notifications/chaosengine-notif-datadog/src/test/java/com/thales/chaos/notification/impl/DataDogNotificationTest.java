package com.thales.chaos.notification.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.ChaosExperimentEvent;
import com.thales.chaos.notification.ChaosMessage;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.platform.Platform;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class DataDogNotificationTest {
    private ChaosExperimentEvent chaosExperimentEvent;
    private ChaosMessage chaosMessage;
    @Mock
    private Platform platform;
    @Mock
    private Container container;
    private DataDogNotification.DataDogEvent dataDogEvent ;

    private String experimentId=UUID.randomUUID().toString();
    private String experimentMethod=UUID.randomUUID().toString();
    private String message = UUID.randomUUID().toString();
    private String title = UUID.randomUUID().toString();


    private NotificationLevel level = NotificationLevel.WARN;
    private ArrayList<String> expectedTagsEvent = new ArrayList<>();
    @Before
    public void setUp () {
        chaosExperimentEvent = ChaosExperimentEvent.builder()
                                                   .withMessage(message)
                                                   .withExperimentId(experimentId)
                                                   .withMessage(message)
                                                   .withNotificationLevel(level)
                                                   .withTargetContainer(container)
                                                   .withExperimentMethod(experimentMethod)
                                                   .withExperimentType(ExperimentType.STATE)
                                                   .build();
        when(container.getSimpleName()).thenReturn(UUID.randomUUID().toString());
        when(container.getPlatform()).thenReturn(platform);
        when(platform.getPlatformType()).thenReturn("TYPE");
        dataDogEvent = new DataDogNotification().new DataDogEvent();
        expectedTagsEvent.add("targetContainer:" + chaosExperimentEvent.getTargetContainer());
        expectedTagsEvent.add("experimentId:" + chaosExperimentEvent.getExperimentId());
        expectedTagsEvent.add("message:" + chaosExperimentEvent.getMessage());
        expectedTagsEvent.add("experimentType:" + chaosExperimentEvent.getExperimentType().name());
        expectedTagsEvent.add("experimentMethod:" + chaosExperimentEvent.getExperimentMethod());
        expectedTagsEvent.add("notificationLevel:" + chaosExperimentEvent.getNotificationLevel());
        chaosMessage = ChaosMessage.builder()
                                   .withMessage(message)
                                   .withTitle(title)
                                   .withNotificationLevel(level)
                                   .build();

    }
    @Test
    public void logEvent(){
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logEvent(chaosExperimentEvent);
        verify(client,times(1)).recordEvent(ArgumentMatchers.any(),ArgumentMatchers.any());
    }

    @Test
    public void logEventFailure(){
            StatsDClient client = Mockito.mock(StatsDClient.class);
            doThrow(StatsDClientException.class).when(client).recordEvent(ArgumentMatchers.any(), ArgumentMatchers.any());
            DataDogNotification notif = new DataDogNotification(client);
        notif.logEvent(chaosExperimentEvent);
    }

    @Test
    public void mapLevel(){
        assertEquals(Event.AlertType.WARNING,dataDogEvent.mapLevel(NotificationLevel.WARN));
        assertEquals(Event.AlertType.ERROR,dataDogEvent.mapLevel(NotificationLevel.ERROR));
        assertEquals(Event.AlertType.SUCCESS,dataDogEvent.mapLevel(NotificationLevel.GOOD));
    }
    @Test
    public void buildEvent(){
        Event expectedEvent = Event.builder()
                                   .withText(message)
                                   .withTitle(DataDogNotification.DataDogEvent.EVENT_PREFIX+ experimentMethod)
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        Event actualEvent = dataDogEvent.buildFromEvent(chaosExperimentEvent);
        assertEquals(expectedEvent.getTitle(), actualEvent.getTitle());
        assertEquals(expectedEvent.getText(), actualEvent.getText());
        assertEquals(expectedEvent.getAlertType(), actualEvent.getAlertType());
        assertEquals(expectedEvent.getSourceTypeName(), actualEvent.getSourceTypeName());
    }

    @Test
    public void buildMessage () {
        Event expectedEvent = Event.builder()
                                   .withText(message)
                                   .withTitle(title)
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        Event actualEvent = dataDogEvent.buildFromNotification(chaosMessage);
        assertEquals(expectedEvent.getTitle(), actualEvent.getTitle());
        assertEquals(expectedEvent.getText(), actualEvent.getText());
        assertEquals(expectedEvent.getAlertType(), actualEvent.getAlertType());
        assertEquals(expectedEvent.getSourceTypeName(), actualEvent.getSourceTypeName());
    }

    @Test
    public void getTags(){
        ArrayList<String> actualTags = new ArrayList<>(Arrays.asList(dataDogEvent.generateTags(chaosExperimentEvent)));
        assertThat(actualTags, is(expectedTagsEvent));
        System.out.println(chaosExperimentEvent);
    }
}