package com.gemalto.chaos.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CalendarUtilsTest {
    @Test
    public void testIncrementCalendarToMidnight () throws Exception {
        Calendar calendar = Mockito.mock(Calendar.class);
        CalendarUtils.incrementCalendarToMidnight(calendar);
        verify(calendar).add(eq(Calendar.DAY_OF_YEAR), eq(1));
        verify(calendar).set(eq(Calendar.HOUR_OF_DAY), eq(0));
        verify(calendar).set(eq(Calendar.MINUTE), eq(0));
        verify(calendar).set(eq(Calendar.SECOND), eq(0));
        verify(calendar).set(eq(Calendar.MILLISECOND), eq(0));
    }
}