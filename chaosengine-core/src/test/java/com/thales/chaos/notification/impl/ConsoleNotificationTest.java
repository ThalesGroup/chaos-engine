package com.thales.chaos.notification.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringJUnit4ClassRunner.class)
public class ConsoleNotificationTest {
    private static final String MESSAGE = StringUtils.generateRandomString(50);
    @Mock
    private ChaosNotification chaosNotification;
    private ConsoleNotification consoleNotification;

    @Before
    public void setUp () {
        consoleNotification = Mockito.spy(new ConsoleNotification());
        when(chaosNotification.getMessage()).thenReturn(MESSAGE);
    }

    @Test
    public void testLogEvent () {
        Logger logger = (Logger) LoggerFactory.getLogger(ConsoleNotification.class);
        ArgumentCaptor<ILoggingEvent> iLoggingEventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
        @SuppressWarnings("unchecked") final Appender<ILoggingEvent> appender = mock(Appender.class);
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        consoleNotification.logNotification(chaosNotification);
        verify(chaosNotification, times(1)).getMessage();
        verify(appender, times(1)).doAppend(iLoggingEventCaptor.capture());
        ILoggingEvent iLoggingEvent = iLoggingEventCaptor.getValue();
        assertEquals(MESSAGE, iLoggingEvent.getMessage());
        assertEquals(Level.DEBUG, iLoggingEvent.getLevel());
    }
}