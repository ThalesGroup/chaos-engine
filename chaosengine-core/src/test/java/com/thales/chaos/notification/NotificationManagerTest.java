package com.thales.chaos.notification;

import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.notification.message.ChaosMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotificationManagerTest {
    private Collection<NotificationMethods> notificationMethodList = new ArrayList<>();
    @Mock
    private NotificationMethods notificationMethod;
    @Mock
    private ChaosExperimentEvent chaosExperimentEvent;
    @Mock
    private ChaosMessage chaosMessage;

    @Before
    public void setUp () {
        notificationMethodList.add(notificationMethod);
    }

    @Test
    public void sendMessage () {
        NotificationManager notificationManager = new NotificationManager(notificationMethodList);
        notificationManager.sendNotification(chaosMessage);
        verify(notificationMethod, times(1)).logNotification(chaosMessage);
    }

    @Test
    public void sendEvent () {
        NotificationManager notificationManager = new NotificationManager(notificationMethodList);
        notificationManager.sendNotification(chaosExperimentEvent);
        verify(notificationMethod, times(1)).logNotification(chaosExperimentEvent);
    }
}