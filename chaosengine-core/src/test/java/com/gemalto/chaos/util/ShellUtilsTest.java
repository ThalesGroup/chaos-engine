package com.gemalto.chaos.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShellUtilsTest {
    @Test
    public void isTarSuccessful () {
        int[] failCodes = { Integer.MIN_VALUE, Integer.MAX_VALUE, 2, -1, 100, -100 };
        for (int failCode : failCodes) {
            assertFalse(ShellUtils.isTarSuccessful(failCode));
        }
        int[] successCodes = { 1, 0 };
        for (int successCode : successCodes) {
            assertTrue(ShellUtils.isTarSuccessful(successCode));
        }
    }
}