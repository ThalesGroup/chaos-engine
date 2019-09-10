/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.admin;

import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdminManagerTest {
    private final AdminState NEW_STATE = AdminState.STARTED;
    @MockBean
    private NotificationManager notificationManager;
    @SpyBean
    private AdminManager adminManager;
    private ChaosNotification expectedChaosNotification;

    @Before
    public void setUp () {
        expectedChaosNotification = ChaosMessage.builder()
                                                .withNotificationLevel(NotificationLevel.WARN)
                                                .withTitle("State changed")
                                                .withMessage("Chaos Engine admin state has changed: " + NEW_STATE.name())
                                                .build();
    }

    @Test
    public void setAdminStateInner () {
        adminManager.setAdminState(NEW_STATE, false);
        verify(notificationManager, never()).sendNotification(any());
        ArgumentCaptor<ChaosNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(ChaosNotification.class);
        adminManager.setAdminState(NEW_STATE, true);
        verify(notificationManager, times(1)).sendNotification(notificationArgumentCaptor.capture());
        ChaosNotification chaosNotification = notificationArgumentCaptor.getValue();
        assertEquals(chaosNotification.asMap(), expectedChaosNotification.asMap());
    }

    @Test
    public void setAdminState () {
        ArgumentCaptor<ChaosNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(ChaosNotification.class);
        adminManager.setAdminState(NEW_STATE);
        verify(notificationManager, times(1)).sendNotification(notificationArgumentCaptor.capture());
        ChaosNotification chaosNotification = notificationArgumentCaptor.getValue();
        assertEquals(chaosNotification.asMap(), expectedChaosNotification.asMap());
        assertEquals(NEW_STATE, adminManager.getAdminState());
    }
}