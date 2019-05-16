package com.thales.chaos.notification;

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
    private ChaosEvent chaosEvent;

    @Test
    public void sendNotification () {
        NotificationManager notificationManager = new NotificationManager(notificationMethodList);
        when(notificationMethodList.iterator()).thenReturn(notificationMethodsIterator);
        when(notificationMethodsIterator.hasNext()).thenReturn(true, false);
        when(notificationMethodsIterator.next()).thenReturn(notificationMethod, notificationMethod);
        notificationManager.sendNotification(chaosEvent);
        verify(notificationMethod, times(1)).logEvent(chaosEvent);
    }
}