package com.thales.chaos.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingManager {
    private static final Logger CHAOS_LOGGER = (Logger) LoggerFactory.getLogger("com.thales.chaos");
    private static final Duration DEFAULT_DEBUG_TIMEOUT = Duration.ofMinutes(30);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedule;

    void setDebugMode () {
        setDebugMode(DEFAULT_DEBUG_TIMEOUT);
    }

    void setDebugMode (Duration timeout) {
        CHAOS_LOGGER.setLevel(Level.DEBUG);
        stopScheduledClear();
        schedule = executor.schedule(this::clearDebugMode, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void stopScheduledClear () {
        if (schedule == null) return;
        if (!schedule.isDone()) schedule.cancel(true);
        schedule = null;
    }

    void clearDebugMode () {
        CHAOS_LOGGER.setLevel(null);
        schedule.cancel(false);
    }

    @PreDestroy
    private void closeExecutor () {
        executor.shutdown();
    }
}
