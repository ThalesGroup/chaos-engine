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
        await().pollInterval(10, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).atLeast(100, TimeUnit.MILLISECONDS).until(() -> logger.getLevel() == null);
        verify(loggingManager, times(1)).clearDebugMode();
    }

    @Test
    public void setDebugModeBackToBack () {
        loggingManager.setDebugMode();
        loggingManager.setDebugMode(Duration.ofMillis(200));
        assertEquals(Level.DEBUG, logger.getLevel());
        assertEquals(Level.DEBUG, ((Logger) LoggerFactory.getLogger(getClass())).getEffectiveLevel());
        await().pollInterval(10, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).atLeast(100, TimeUnit.MILLISECONDS).until(() -> logger.getLevel() == null);
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