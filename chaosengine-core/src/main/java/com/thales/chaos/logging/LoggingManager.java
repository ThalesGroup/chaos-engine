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

package com.thales.chaos.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingManager {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(LoggingManager.class);
    private static final Logger CHAOS_LOGGER = (Logger) LoggerFactory.getLogger("com.thales.chaos");
    private static final Duration DEFAULT_DEBUG_TIMEOUT = Duration.ofMinutes(30);
    private static final Marker ALWAYS = MarkerFactory.getMarker("ALWAYS");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedule;

    void setDebugMode () {
        setDebugMode(DEFAULT_DEBUG_TIMEOUT);
    }

    void setDebugMode (Duration timeout) {
        logger.info(ALWAYS, "Entering debug mode for {}", timeout);
        CHAOS_LOGGER.setLevel(Level.DEBUG);
        stopScheduledClear(true);
        schedule = executor.schedule(this::clearDebugMode, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void stopScheduledClear (boolean mayInterruptIfRunning) {
        if (schedule == null) return;
        if (!schedule.isDone()) schedule.cancel(mayInterruptIfRunning);
        schedule = null;
    }

    void clearDebugMode () {
        logger.info(ALWAYS, "Debug mode expired");
        CHAOS_LOGGER.setLevel(null);
        stopScheduledClear(false);
    }

    @PreDestroy
    private void closeExecutor () {
        executor.shutdown();
    }
}
