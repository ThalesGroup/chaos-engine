package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.gemalto.chaos.platform.Platform;
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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class DataDogNotificationTest {
    private ChaosEvent chaosEvent;
    @Mock
    private Platform platform;
    @Mock
    private Container container;
    private DataDogNotification.DataDogEvent dataDogEvent ;

    private String experimentId=UUID.randomUUID().toString();
    private String experimentMethod=UUID.randomUUID().toString();
    private String message = UUID.randomUUID().toString();


    private NotificationLevel level = NotificationLevel.WARN;
    private ArrayList<String> expectedTags = new ArrayList<>();
    @Before
    public void setUp () {
        chaosEvent= ChaosEvent.builder()
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
        dataDogEvent= new DataDogNotification().new DataDogEvent(chaosEvent);


        expectedTags.add(DataDogNotification.DataDogEvent.EXPERIMENT_ID + chaosEvent.getExperimentId());
        expectedTags.add(DataDogNotification.DataDogEvent.METHOD + chaosEvent.getExperimentMethod());
        expectedTags.add(DataDogNotification.DataDogEvent.TYPE + chaosEvent.getExperimentType().name());
        expectedTags.add(DataDogNotification.DataDogEvent.TARGET + chaosEvent.getTargetContainer().getSimpleName());
        expectedTags.add(DataDogNotification.DataDogEvent.PLATFORM + chaosEvent.getTargetContainer().getPlatform().getPlatformType());

    }
    @Test
    public void logEvent(){
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logEvent(chaosEvent);
        verify(client,times(1)).recordEvent(ArgumentMatchers.any(),ArgumentMatchers.any());
    }

    @Test
    public void logEventFailure(){
        try {
            StatsDClient client = Mockito.mock(StatsDClient.class);
            doThrow(StatsDClientException.class).when(client).recordEvent(ArgumentMatchers.any(), ArgumentMatchers.any());
            DataDogNotification notif = new DataDogNotification(client);
            notif.logEvent(chaosEvent);
        }catch (Exception ex){
            fail(ex.getMessage());
        }
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
        Event actualEvent =dataDogEvent.buildEvent();
        assertEquals(expectedEvent.getTitle(),actualEvent.getTitle());
        assertEquals(expectedEvent.getText(),actualEvent.getText());
        assertEquals(expectedEvent.getAlertType(),actualEvent.getAlertType());
        assertEquals(expectedEvent.getSourceTypeName(),actualEvent.getSourceTypeName());
    }

    @Test
    public void getTags(){
        ArrayList<String> actualTags= new ArrayList<>(Arrays.asList(dataDogEvent.generateTags()));
        assertThat(expectedTags,is(actualTags));
    }
}