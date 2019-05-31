package com.thales.chaos.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.notification.enums.NotificationLevel;

import java.util.Map;

public interface ChaosNotification {
    String getTitle ();

    String getMessage ();

    NotificationLevel getNotificationLevel ();

    @JsonIgnore
    @SuppressWarnings("unchecked")
    default Map<Object, Object> asMap () {
        return (Map<Object, Object>) new ObjectMapper().convertValue(this, Map.class);
    }
}
