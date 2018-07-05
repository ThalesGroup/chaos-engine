package com.gemalto.chaos.logging;

import com.gemalto.chaos.logging.enums.LoggingLevel;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class LoggingControllerTest {
    private static final String OTHER_CLASS = "org.springframework";
    private static final Logger GEMALTO_LOGGER = LoggerFactory.getLogger("com.gemalto");
    private static final Logger OTHER_LOGGER = LoggerFactory.getLogger(OTHER_CLASS);
    private LoggingController loggingController;

    @Before
    public void setUp () {
        loggingController = new LoggingController();
    }

    @Test
    public void loggingController () {
        loggingController.setLogLevel(LoggingLevel.DEBUG, OTHER_CLASS);
        assertEquals("DEBUG", loggingController.getLogLevel(OTHER_CLASS));
        assertTrue(OTHER_LOGGER.isDebugEnabled());
        loggingController.setLogLevel(LoggingLevel.ERROR);
        assertEquals("ERROR", loggingController.getLogLevel());
        assertTrue(GEMALTO_LOGGER.isErrorEnabled());
        assertFalse(GEMALTO_LOGGER.isWarnEnabled());
    }
}