package com.thales.chaos.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class LoggingManagerTest {
    private static final String pkg = "com.thales.chaos";
    private static final Logger logger = (Logger) LoggerFactory.getLogger(pkg);
    @Autowired
    private LoggingManager loggingManager;

    @Before
    public void setUp () {
        ensureLoggersDisabled();
        reset(loggingManager);
    }

    private void ensureLoggersDisabled () {
        assertNull(logger.getLevel());
        assertEquals(Level.OFF, logger.getEffectiveLevel());
    }

    @After
    public void tearDown () {
        logger.setLevel(null);
        ensureLoggersDisabled();
    }

    @Test
    public void setDebugMode () {
        loggingManager.setDebugMode();
        assertEquals(Level.DEBUG, logger.getLevel());
        assertEquals(Level.DEBUG, ((Logger) LoggerFactory.getLogger(getClass())).getEffectiveLevel());
    }

    @Test
    public void setDebugModeWithNoTimeout () {
        loggingManager.setDebugMode(Duration.ofMillis(200));
        assertEquals(Level.DEBUG, logger.getLevel());
        assertEquals(Level.DEBUG, ((Logger) LoggerFactory.getLogger(getClass())).getEffectiveLevel());
        await().pollInterval(10, TimeUnit.MILLISECONDS)
               .atMost(1, TimeUnit.SECONDS)
               .atLeast(100, TimeUnit.MILLISECONDS)
               .until(() -> logger.getLevel() == null);
        verify(loggingManager, times(1)).clearDebugMode();
    }

    @Test
    public void setDebugModeBackToBack () {
        loggingManager.setDebugMode();
        loggingManager.setDebugMode(Duration.ofMillis(200));
        assertEquals(Level.DEBUG, logger.getLevel());
        assertEquals(Level.DEBUG, ((Logger) LoggerFactory.getLogger(getClass())).getEffectiveLevel());
        await().pollInterval(10, TimeUnit.MILLISECONDS)
               .atMost(1, TimeUnit.SECONDS)
               .atLeast(100, TimeUnit.MILLISECONDS)
               .until(() -> logger.getLevel() == null);
        verify(loggingManager, times(1)).clearDebugMode();
    }

    @Configuration
    static class LoggingManagerTestConfiguration {
        @Bean
        public LoggingManager loggingManager () {
            return spy(new LoggingManager());
        }
    }
}