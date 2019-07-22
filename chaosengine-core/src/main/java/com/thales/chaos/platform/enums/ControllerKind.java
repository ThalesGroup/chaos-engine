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
    CRON_JOB("CronJob");
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
        return invertedStringMap.get(string);
    }
}
