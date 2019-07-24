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
