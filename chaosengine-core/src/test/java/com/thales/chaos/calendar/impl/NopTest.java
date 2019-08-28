package com.thales.chaos.calendar.impl;

import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;

import static org.junit.Assert.*;

public class NopTest {
    Nop calendar = new Nop();

    @Test
    public void isHoliday () {
        assertTrue(calendar.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
    }

    private Calendar getDate (int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c;
    }

    @Test
    public void isWorkingHours () {
        // June 21, 2018 10:26:45 AM GMT
        assertFalse(calendar.isWorkingHours(Instant.ofEpochSecond(1529569605)));
    }

    @Test
    public void getStartOfDay () {
        assertEquals(0, calendar.getStartOfDay());
    }

    @Test
    public void getEndOfDay () {
        assertEquals(24, calendar.getEndOfDay());
    }

    @Test
    public void isBeforeWorkingHours () {
        assertTrue(calendar.isBeforeWorkingHours(getDate(2018, Calendar.OCTOBER, 20)));
    }

    @Test
    public void isWeekend () {
        assertTrue(calendar.isWeekend(getDate(2018, Calendar.OCTOBER, 20)));
    }
}