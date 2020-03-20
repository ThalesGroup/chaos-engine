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

package com.thales.chaos.container.enums;

import java.util.HashMap;
import java.util.Map;

public enum CloudFoundryApplicationRouteType {
    /*
      If the route is of the HTTP type the API returns null
      That's why the empty string is used because null can't be used as a key in the reverse map
    */
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
