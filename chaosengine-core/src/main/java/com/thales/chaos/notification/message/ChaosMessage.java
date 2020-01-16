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

package com.thales.chaos.notification.message;

import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.enums.NotificationLevel;

public class ChaosMessage implements ChaosNotification {
    private String title;
    private String message;
    private NotificationLevel notificationLevel;
    public static final String CHAOS_MESSAGE_PREFIX = "Chaos Message";

    public static ChaosMessageBuilder builder () {
        return ChaosMessageBuilder.builder();
    }

    @Override
    public String getTitle () {
        return title;
    }

    @Override
    public String getMessage () {
        return message;
    }

    @Override
    public NotificationLevel getNotificationLevel () {
        return notificationLevel;
    }

    public static final class ChaosMessageBuilder {
        private String title = CHAOS_MESSAGE_PREFIX;
        private String message;
        private NotificationLevel notificationLevel;

        private ChaosMessageBuilder () {
        }

        private static ChaosMessageBuilder builder () {
            return new ChaosMessageBuilder();
        }

        public ChaosMessageBuilder withTitle (String title) {
            this.title = CHAOS_MESSAGE_PREFIX + " - " + title;
            return this;
        }

        public ChaosMessageBuilder withMessage (String message) {
            this.message = message;
            return this;
        }

        public ChaosMessageBuilder withNotificationLevel (NotificationLevel notificationLevel) {
            this.notificationLevel = notificationLevel;
            return this;
        }

        public ChaosMessage build () {
            ChaosMessage chaosMessage = new ChaosMessage();
            chaosMessage.title = this.title;
            chaosMessage.message = this.message;
            chaosMessage.notificationLevel = this.notificationLevel;
            return chaosMessage;
        }
    }
}
