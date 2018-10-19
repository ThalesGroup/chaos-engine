package com.gemalto.chaos.calendar.impl;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;

import static org.junit.Assert.*;

public class DummyTest {
    Dummy calendar = new Dummy();

    private Calendar getDate (int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c;
    }
    @Test
    public void isHoliday () {
        assertFalse(calendar.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
    }

    @Test
    public void isWorkingHours () {
        // June 21, 2018 10:26:45 AM GMT
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529569605)));
        // June 21, 2018 11:13:18 AM
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529579598)));
        //June 21, 2018 4:59:59 PM GMT
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529593199)));
        //  June 21, 2018 5:00:00 PM GMT  (One second difference from above)
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529593200)));
        //  June 21, 2018 6:00:00 AM GMT
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529553600)));
        //  June 21, 2018 6:00:00 PM GMT
        assertTrue(calendar.isWorkingHours(Instant.ofEpochSecond(1529596800)));
    }

    @Test
    public void getStartOfDay () {
        assertEquals(0,calendar.getStartOfDay());
    }

    @Test
    public void getEndOfDay () {
        assertEquals(24,calendar.getEndOfDay());
    }

    @Test
    public void isBeforeWorkingHours () {
        assertFalse(calendar.isBeforeWorkingHours(getDate(2018, Calendar.OCTOBER, 20)));
    }

    @Test
    public void isWeekend () {
        assertFalse(calendar.isWeekend(getDate(2018, Calendar.OCTOBER, 20)));
    }
}