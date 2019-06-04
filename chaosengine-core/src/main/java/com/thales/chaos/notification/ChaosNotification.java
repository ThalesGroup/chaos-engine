package com.thales.chaos.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.notification.enums.NotificationLevel;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public interface ChaosNotification {
    String getTitle ();

    String getMessage ();

    NotificationLevel getNotificationLevel ();

    @JsonIgnore
    @SuppressWarnings("unchecked")
    default Map<String, Object> asMap () {
        TreeMap<String, Object> treeMap = new TreeMap<>(Comparator.naturalOrder());
        treeMap.putAll((Map<String, Object>) new ObjectMapper().convertValue(this, Map.class));
        return treeMap;
    }
}
