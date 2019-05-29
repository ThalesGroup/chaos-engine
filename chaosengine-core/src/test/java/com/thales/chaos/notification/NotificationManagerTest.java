package com.thales.chaos.notification;

import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.notification.message.ChaosMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationManagerTest {
    @Mock
    private List<NotificationMethods> notificationMethodList;
    @Mock
    private Iterator<NotificationMethods> notificationMethodsIterator;
    @Mock
    private NotificationMethods notificationMethod;
    @Mock
    private ChaosExperimentEvent chaosExperimentEvent;
    @Mock
    private ChaosMessage chaosMessage;

    @Test
    public void sendNotification () {
        NotificationManager notificationManager = new NotificationManager(notificationMethodList);
        when(notificationMethodList.iterator()).thenReturn(notificationMethodsIterator, notificationMethodsIterator);
        when(notificationMethodsIterator.hasNext()).thenReturn(true, false, true, false);
        when(notificationMethodsIterator.next()).thenReturn(notificationMethod, notificationMethod);
        notificationManager.sendNotification(chaosExperimentEvent);
        verify(notificationMethod, times(1)).logEvent(chaosExperimentEvent);
        notificationManager.sendNotification(chaosMessage);
        verify(notificationMethod, times(1)).logMessage(chaosMessage);
    }
}