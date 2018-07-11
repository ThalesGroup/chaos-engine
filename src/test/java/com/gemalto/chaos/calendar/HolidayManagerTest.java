package com.gemalto.chaos.calendar;

import com.gemalto.chaos.calendar.impl.Canada;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

public class HolidayManagerTest {
    private HolidayManager holidayManager;

    @Before
    public void setUp () {
        holidayManager = new HolidayManager(new Canada());
    }

    @Test
    public void getOneWorkingDayAgo () {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 29);
        c.set(Calendar.MONTH, Calendar.JUNE);
        c.set(Calendar.YEAR, 2018);
        Instant expected = c.toInstant().truncatedTo(ChronoUnit.DAYS);
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.DAY_OF_MONTH, 3);
        actual.set(Calendar.MONTH, Calendar.JULY);
        actual.set(Calendar.YEAR, 2018);
        Instant previousWorkingDay = holidayManager.getPreviousWorkingDay(actual).truncatedTo(ChronoUnit.DAYS);
        Assert.assertEquals(expected, previousWorkingDay);
    }
}