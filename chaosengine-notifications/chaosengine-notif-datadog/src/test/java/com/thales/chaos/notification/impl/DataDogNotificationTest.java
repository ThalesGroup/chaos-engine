package com.thales.chaos.notification.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.ChaosExperimentEvent;
import com.thales.chaos.notification.ChaosMessage;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.util.StringUtils;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
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
    private String message = StringUtils.generateRandomString(50);
    private String title = StringUtils.generateRandomString(50);
    private String target = UUID.randomUUID().toString();
    private String platformType = "Platform";
    private String conatainerType = "Container";
    private String aggregationIdentifier = conatainerType;


    private NotificationLevel level = NotificationLevel.WARN;
    private ArrayList<String> expectedTagsEvent = new ArrayList<>();
    private ArrayList<String> expectedTagsMessage = new ArrayList<>();
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
        when(container.getSimpleName()).thenReturn(target);
        when(container.getPlatform()).thenReturn(platform);
        when(container.getAggregationIdentifier()).thenReturn(aggregationIdentifier);
        when(container.getContainerType()).thenReturn(conatainerType);
        when(platform.getPlatformType()).thenReturn(platformType);
        dataDogEvent = new DataDogNotification().new DataDogEvent();
        expectedTagsEvent.add("experimentId:" + chaosExperimentEvent.getExperimentId());
        expectedTagsEvent.add("experimentType:" + chaosExperimentEvent.getExperimentType().name());
        expectedTagsEvent.add("experimentMethod:" + chaosExperimentEvent.getExperimentMethod());
        expectedTagsEvent.add("notificationLevel:" + chaosExperimentEvent.getNotificationLevel());
        expectedTagsEvent.add("target:" + target);
        expectedTagsEvent.add("aggregationidentifier:" + aggregationIdentifier);
        expectedTagsEvent.add("platform:" + platformType);
        expectedTagsEvent.add("containertype:" + conatainerType);
        chaosMessage = ChaosMessage.builder()
                                   .withMessage(message)
                                   .withTitle(title)
                                   .withNotificationLevel(level)
                                   .build();
        expectedTagsMessage.add("notificationLevel:" + level);
    }
    @Test
    public void logEvent(){
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logEvent(chaosExperimentEvent);
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Event expectedEvent = Event.builder()
                                   .withAggregationKey(chaosExperimentEvent.getExperimentId())
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withTitle(chaosExperimentEvent.getTitle())
                                   .withText(chaosExperimentEvent.getMessage())
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        verify(client, times(1)).recordEvent(eventCaptor.capture(), tagsCaptor.capture());
        assertThat(tagsCaptor.getAllValues(), is(expectedTagsEvent));
        Event actualEvent = eventCaptor.getValue();
        assertEquals(actualEvent.getAggregationKey(), expectedEvent.getAggregationKey());
        assertEquals(actualEvent.getAlertType(), expectedEvent.getAlertType());
        assertEquals(actualEvent.getTitle(), expectedEvent.getTitle());
        assertEquals(actualEvent.getText(), expectedEvent.getText());
        assertEquals(actualEvent.getSourceTypeName(), expectedEvent.getSourceTypeName());
    }

    @Test
    public void logMessage () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logMessage(chaosMessage);
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Event expectedEvent = Event.builder()
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withTitle(chaosMessage.getTitle())
                                   .withText(chaosMessage.getMessage())
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        verify(client, times(1)).recordEvent(eventCaptor.capture(), tagsCaptor.capture());
        assertThat(tagsCaptor.getAllValues(), is(expectedTagsMessage));
        Event actualEvent = eventCaptor.getValue();
        assertEquals(actualEvent.getAggregationKey(), expectedEvent.getAggregationKey());
        assertEquals(actualEvent.getAlertType(), expectedEvent.getAlertType());
        assertEquals(actualEvent.getTitle(), expectedEvent.getTitle());
        assertEquals(actualEvent.getText(), expectedEvent.getText());
        assertEquals(actualEvent.getSourceTypeName(), expectedEvent.getSourceTypeName());

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
                                   .withText(message).withTitle(ChaosExperimentEvent.CHAOS_EXPERIMENT_EVENT_PREFIX)
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
                                   .withText(message).withTitle(ChaosMessage.CHAOS_MESSAGE_PREFIX + " - " + title)
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
    public void getTagsEvent () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logEvent(chaosExperimentEvent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).recordEvent(ArgumentMatchers.any(), captor.capture());
        assertThat(captor.getAllValues(), is(expectedTagsEvent));
    }

    @Test
    public void getTagsMessage () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logMessage(chaosMessage);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).recordEvent(ArgumentMatchers.any(), captor.capture());
        assertThat(captor.getAllValues(), is(expectedTagsMessage));
    }
}