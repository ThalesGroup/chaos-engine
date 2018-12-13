package com.gemalto.chaos.util;

import java.nio.charset.Charset;
import java.util.Random;

public class StringUtils {
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
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }
}
