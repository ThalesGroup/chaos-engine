package com.gemalto.chaos.calendar.holidaycalendar;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class CanadaTest {

    @Test
    public void isHoliday() {
        HolidayCalendar Canada = new Canada();

        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.JANUARY, 1)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.FEBRUARY, 19)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.MARCH, 30)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.MAY, 21)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.JULY, 1)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.JULY, 2)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.AUGUST, 6)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.SEPTEMBER, 3)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.OCTOBER, 8)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 25)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 26)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 27)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 28)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 29)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 30)));
        Assert.assertTrue(Canada.isHoliday(getDate(2018, Calendar.DECEMBER, 31)));


        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.JANUARY, 1)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.FEBRUARY, 18)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.APRIL, 19)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.MAY, 20)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.JULY, 1)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.AUGUST, 5)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.SEPTEMBER, 2)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.OCTOBER, 14)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 24)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 25)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 26)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 27)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 28)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 29)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 30)));
        Assert.assertTrue(Canada.isHoliday(getDate(2019, Calendar.DECEMBER, 31)));


    }

    private Calendar getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c;
    }
}