package com.gemalto.chaos.util;

public class ShellUtils {
    private ShellUtils () {
    }

    public static boolean isTarSuccessful (int exitCode) {
        return exitCode == 0 || exitCode == 1;
    }
}
