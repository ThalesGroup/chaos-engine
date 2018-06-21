package com.gemalto.chaos.calendar.impl;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;

public class CzechiaTest {

    @Test
    public void isHoliday() {
        HolidayCalendar czechia = new Czechia();

        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.JANUARY, 1)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.MAY, 1)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.MAY, 8)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.JULY, 5)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.JULY, 6)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.SEPTEMBER, 28)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.OCTOBER, 28)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.NOVEMBER, 17)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.DECEMBER, 25)));
        Assert.assertTrue(czechia.isHoliday(getDate(2018, Calendar.DECEMBER, 26)));
    }

    @Test
    public void isWorkingHours() {
        HolidayCalendar czechia = new Czechia();

        // June 21, 2018 10:26:45 AM GMT (True)
        Assert.assertTrue(czechia.isWorkingHours(Instant.ofEpochSecond(1529569605)));

        // June 21, 2018 11:13:18 AM (True)
        Assert.assertTrue(czechia.isWorkingHours(Instant.ofEpochSecond(1529579598)));

        //June 21, 2018 4:59:59 PM GMT(True)
        Assert.assertTrue(czechia.isWorkingHours(Instant.ofEpochSecond(1529593199)));


        //  June 21, 2018 5:00:00 PM GMT (False) (One second difference from above)
        Assert.assertTrue(!czechia.isWorkingHours(Instant.ofEpochSecond(1529593200)));

        //  June 21, 2018 6:00:00 AM GMT (False)
        Assert.assertTrue(!czechia.isWorkingHours(Instant.ofEpochSecond(1529553600)));

        //  June 21, 2018 6:00:00 PM GMT (False)
        Assert.assertTrue(!czechia.isWorkingHours(Instant.ofEpochSecond(1529596800)));


    }

    private Calendar getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c;
    }
}