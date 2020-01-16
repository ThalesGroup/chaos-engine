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

package com.thales.chaos.util;

import java.util.Random;

public class StringUtils {
    private static final Random RANDOM = new Random();

    private StringUtils () {
    }

    public static String addQuotesIfNecessary (String string) {
        if (!string.contains(" ")) return string;
        StringBuilder sb = new StringBuilder();
        if (!string.startsWith("\"")) sb.append("\"");
        sb.append(string);
        if (!string.endsWith("\"")) sb.append("\"");
        return sb.toString();
    }

    public static String generateRandomString (int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(Character.toString(RANDOM.nextInt(93) + 33));
        return sb.toString();
    }

    public static String convertCamelCaseToSentence (String camelCase) {
        return camelCase.substring(0, 1).toUpperCase() + camelCase.substring(1).replaceAll("([A-Z])", " $1");
    }

    public static String trimSpaces (String original) {
        return original.replaceFirst("^\\s*(.*?)\\s*$", "$1");
    }
}
