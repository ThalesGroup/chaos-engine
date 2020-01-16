/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

package com.thales.chaos.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class NotificationManager {
    @Autowired(required = false)
    private Collection<NotificationMethods> notificationMethods;
    @Autowired(required = false)
    private Collection<BufferedNotificationMethod> bufferedNotificationMethods;

    @Autowired
    private NotificationManager () {
    }

    NotificationManager (Collection<NotificationMethods> notificationMethods) {
        this.notificationMethods = notificationMethods;
    }

    public void sendNotification (ChaosNotification chaosNotification) {
        if (notificationMethods != null) {
            for (NotificationMethods notif : notificationMethods) {
                notif.logNotification(chaosNotification);
            }
        }
    }

    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 5)
    public void flushBuffers () {
        if (bufferedNotificationMethods != null) {
            bufferedNotificationMethods.forEach(BufferedNotificationMethod::flushBuffer);
        }
    }
}
