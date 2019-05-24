package com.thales.chaos.notification;

import com.thales.chaos.notification.enums.NotificationLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class ChaosNotification {
    public abstract String getTitle ();

    public abstract String getMessage ();

    public abstract NotificationLevel getNotificationLevel ();

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + ": ");
        for (Field field : this.getClass().getDeclaredFields()) {
            boolean usedField = true;
            field.setAccessible(true);
            try {
                if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()) || field.get(this) == null) {
                    usedField = false;
                }
            } catch (IllegalAccessException e) {
                usedField = false;
            }
            if (!usedField) continue;
            sb.append("[");
            sb.append(field.getName());
            sb.append("=");
            try {
                sb.append(field.get(this));
            } catch (IllegalAccessException e) {
                sb.append("IllegalAccessException");
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
