package com.thales.chaos.util;

public class ShellUtils {
    private ShellUtils () {
    }

    public static boolean isTarSuccessful (int exitCode) {
        return exitCode == 0 || exitCode == 1;
    }

    public static boolean isCommentedLine (String s) {
        return s.startsWith("#");
    }
}
