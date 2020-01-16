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
