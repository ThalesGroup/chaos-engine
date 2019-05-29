package com.thales.chaos.notification.enums;

import java.util.stream.Stream;

public enum NotificationLevel {
    ERROR,
    WARN,
    GOOD;

    public static boolean isNotificationLevel (Object obj) {
        return Stream.of(NotificationLevel.values()).map(Object::toString).anyMatch(obj::equals);
    }
}
