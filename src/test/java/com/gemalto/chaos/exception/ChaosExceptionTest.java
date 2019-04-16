package com.gemalto.chaos.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.gemalto.chaos.exception.enums.ChaosErrorCode.GENERIC_FAILURE;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ChaosExceptionTest {
    private static final String MESSAGE = "This is why bad things happened";

    @Test(expected = ChaosException.class)
    public void emptyConstructor () {
        throw new ChaosException();
    }

    @Test(expected = ChaosException.class)
    public void stringConstructor () {
        ChaosException chaosException = new ChaosException(MESSAGE);
        assertEquals(MESSAGE, chaosException.getMessage());
        throw chaosException;
    }

    @Test(expected = ChaosException.class)
    public void stringThrowableConstructor () {
        Exception causeException = new Exception();
        ChaosException chaosException = new ChaosException(MESSAGE, causeException);
        assertEquals(MESSAGE, chaosException.getMessage());
        assertEquals(causeException, chaosException.getCause());
        throw chaosException;
    }

    @Test(expected = ChaosException.class)
    public void throwableConstructor () {
        Exception causeException = new Exception();
        ChaosException chaosException = new ChaosException(causeException);
        assertSame(causeException, chaosException.getCause());
        assertEquals(GENERIC_FAILURE.getFormattedMessage(), chaosException.getMessage());
        throw chaosException;
    }

    @Test(expected = ChaosException.class)
    public void nestedChaosException () {
        final AtomicBoolean loggerCalled = new AtomicBoolean(false);
        Appender<ILoggingEvent> appender = new AppenderBase<>() {
            @Override
            protected void append (ILoggingEvent loggingEvent) {
                assertEquals(Level.ERROR, loggingEvent.getLevel());
                assertEquals("Rewrapped ChaosException: 10000: A generic error has occurred", loggingEvent.getMessage());
                loggerCalled.set(true);
            }
        };
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        RuntimeException e = new ChaosException(GENERIC_FAILURE, new ChaosException(GENERIC_FAILURE));
        assertTrue("Logger should have been called", loggerCalled.get());
        throw e;
    }
}