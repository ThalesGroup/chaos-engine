package com.thales.chaos.util;

import com.thales.chaos.ChaosEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ErrorUtils {
    private static final Class ROOT_PACKAGE = ChaosEngine.class;

    private ErrorUtils () {
    }

    public static void logStacktraceUsingLastValidLogger (Throwable ex) {
        logStacktraceUsingLastValidLogger(ex, "Uncaught " + ex.getClass().getSimpleName());
    }

    public static void logStacktraceUsingLastValidLogger (Throwable ex, String errorMessage) {
        final Logger logger = Arrays.stream(ex.getStackTrace())
                                    .map(StackTraceElement::getClassName)
                                    .filter(s -> s.startsWith(ROOT_PACKAGE.getPackage().getName()))
                                    .findFirst()
                                    .map(LoggerFactory::getLogger)
                                    .orElseGet(() -> LoggerFactory.getLogger(ROOT_PACKAGE));
        logger.error(errorMessage, ex);
    }
}
