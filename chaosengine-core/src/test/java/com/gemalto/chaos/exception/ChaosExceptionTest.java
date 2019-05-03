package com.gemalto.chaos.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.gemalto.chaos.exception.enums.ChaosErrorCode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static com.gemalto.chaos.exception.enums.ChaosErrorCode.API_EXCEPTION;
import static com.gemalto.chaos.exception.enums.ChaosErrorCode.GENERIC_FAILURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
        final ChaosErrorCode errorCode = GENERIC_FAILURE;
        final ChaosException cause = new ChaosException(errorCode);
        final String expected = String.format("Rewrapped %s: %s", cause.getClass()
                                                                       .getSimpleName(), errorCode.getFormattedMessage());
        final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        final ArgumentCaptor<ILoggingEvent> iLoggingEventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
        @SuppressWarnings("unchecked") final Appender<ILoggingEvent> appender = mock(Appender.class);
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        final RuntimeException e = new ChaosException(API_EXCEPTION, cause);
        Mockito.verify(appender, times(1)).doAppend(iLoggingEventCaptor.capture());
        final ILoggingEvent loggingEvent = iLoggingEventCaptor.getValue();
        assertEquals(Level.ERROR, loggingEvent.getLevel());
        assertEquals(expected, loggingEvent.getMessage());
        throw e;
    }

    @Test(expected = ChaosException.class)
    public void nestedRuntimeException () {
        final Exception cause = new RuntimeException();
        final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
        @SuppressWarnings("unchecked") final Appender<ILoggingEvent> appender = mock(Appender.class);
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        final RuntimeException e = new ChaosException(API_EXCEPTION, cause);
        Mockito.verify(appender, never()).doAppend(any());
        throw e;
    }

}