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
