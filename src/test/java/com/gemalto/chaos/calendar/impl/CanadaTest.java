package com.gemalto.chaos.calendar.impl;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class CanadaTest {
    private HolidayCalendar holidayCalendar;

    @Before
    public void setUp () {
        holidayCalendar = new Canada();
    }

    @Test
    public void isHoliday () {
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.JANUARY, 1)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.FEBRUARY, 19)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.MARCH, 30)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.MAY, 21)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.JULY, 1)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.JULY, 2)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.AUGUST, 6)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.SEPTEMBER, 3)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.OCTOBER, 8)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 25)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 26)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 27)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 28)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 29)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 30)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2018, Calendar.DECEMBER, 31)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.JANUARY, 1)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.FEBRUARY, 18)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.APRIL, 19)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.MAY, 20)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.JULY, 1)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.AUGUST, 5)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.SEPTEMBER, 2)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.OCTOBER, 14)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 24)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 25)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 26)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 27)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 28)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 29)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 30)));
        Assert.assertTrue(holidayCalendar.isHoliday(getDate(2019, Calendar.DECEMBER, 31)));
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
        // 2018-06-13 13:45:49 GMT (True)
        Assert.assertTrue(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1528897555)));
        // 2018-06-13 20:59:59 GMT (True)
        Assert.assertTrue(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1528923599)));
        // 2018-06-13 21:00:00 GMT (False) (One second difference from above)
        Assert.assertTrue(!holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1528923600)));
        // 2018-06-13 12:14:43 GMT (False)
        Assert.assertTrue(!holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1528892083)));
        // 2018-06-10 12:00:00 Eastern (False)
        Assert.assertTrue(!holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1528646400)));
    }

    @Test
    public void getTimeZone () {
        assertEquals(TimeZone.getTimeZone("America/Toronto"), holidayCalendar.getTimeZone());
    }
}
