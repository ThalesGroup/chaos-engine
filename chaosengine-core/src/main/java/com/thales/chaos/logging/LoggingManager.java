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
