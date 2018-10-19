package com.gemalto.chaos.notification.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import com.timgroup.statsd.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class DataDogNotificationTest {
    ChaosEvent chaosEvent;
    @Mock
    Container container;
    DataDogNotification dataDogNotification = new DataDogNotification();
    DataDogNotification.DataDogEvent dataDogEvent ;

    String experimentId=UUID.randomUUID().toString();
    String experimentMethod=UUID.randomUUID().toString();
    String message = UUID.randomUUID().toString();

    String sourcetypeName="Java";

    NotificationLevel level = NotificationLevel.WARN;
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
        when(container.getSimpleName()).thenReturn("");
        dataDogEvent= new DataDogNotification().new DataDogEvent(chaosEvent);

    }
    @Test
    public void logEvent(){
        dataDogNotification.logEvent(chaosEvent);
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
                                   .withTitle(dataDogEvent.eventPrefix+ experimentMethod)
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withSourceTypeName(sourcetypeName)
                                   .build();
        Event actualEvent =dataDogEvent.buildEvent();
        assertEquals(expectedEvent.getTitle(),actualEvent.getTitle());
        assertEquals(expectedEvent.getText(),actualEvent.getText());
        assertEquals(expectedEvent.getAlertType(),actualEvent.getAlertType());
        assertEquals(expectedEvent.getSourceTypeName(),actualEvent.getSourceTypeName());
    }
}