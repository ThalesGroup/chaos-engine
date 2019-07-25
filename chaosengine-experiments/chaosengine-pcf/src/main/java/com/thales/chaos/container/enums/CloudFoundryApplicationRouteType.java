package com.thales.chaos.container.enums;

import java.util.HashMap;
import java.util.Map;

public enum CloudFoundryApplicationRouteType {
    HTTP(""),
    TCP("tcp"),
    UNKNOWN("unknown");
    private static final Map<String, CloudFoundryApplicationRouteType> invertedStringMap;

    static {
        invertedStringMap = new HashMap<>();
        for (CloudFoundryApplicationRouteType value : values()) {
            invertedStringMap.put(value.rawName, value);
        }
    }

    private final String rawName;

    CloudFoundryApplicationRouteType (String rawName) {
        this.rawName = rawName;
    }

    public static CloudFoundryApplicationRouteType mapFromString (String string) {
        // PCF domain.getType() API call which is used to determine route type
        // returns null if the selected application route is of the HTTP type
        // It is not possible to have a null as a Map key thus following extra handling was added
        if (string == null) {
            return HTTP;
        }
        return invertedStringMap.getOrDefault(string, UNKNOWN);
    }
}
