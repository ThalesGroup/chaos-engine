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
        return invertedStringMap.getOrDefault(string, UNKNOWN);
    }
}
