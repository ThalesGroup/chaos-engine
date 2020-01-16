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

package com.thales.chaos.platform.enums;

import java.util.HashMap;
import java.util.Map;

public enum ControllerKind {
    REPLICATION_CONTROLLER("ReplicationController"),
    REPLICA_SET("ReplicaSet"),
    DEPLOYMENT("Deployment"),
    STATEFUL_SET("StatefulSet"),
    DAEMON_SET("DaemonSet"),
    JOB("Job"),
    CRON_JOB("CronJob"),
    UNKNOWN("Unknown");
    private static final Map<String, ControllerKind> invertedStringMap;

    static {
        invertedStringMap = new HashMap<>();
        for (ControllerKind value : values()) {
            invertedStringMap.put(value.rawName, value);
        }
    }

    private final String rawName;

    ControllerKind (String rawName) {
        this.rawName = rawName;
    }

    public static ControllerKind mapFromString (String string) {
        return invertedStringMap.getOrDefault(string, UNKNOWN);
    }
}
