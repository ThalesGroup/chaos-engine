package com.gemalto.chaos.calendar.impl;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FranceTest {
    @Test
    public void isHoliday () {
        HolidayCalendar France = new France();
        // Test 2018 Holidays
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.JANUARY, 1)));
        // Good Friday, Easter Monday
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MARCH, 30)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.APRIL, 2)));
        // May Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 1)));
        // Victory in Europe Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 8)));
        // Monday prior to it
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 7)));
        // Ascension Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 10)));
        // Friday after it
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 11)));
        // Whit Monday
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.MAY, 21)));
        // Bastille Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.JULY, 14)));
        // Friday Before
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.JULY, 13)));
        // Assumption of Mary to Heaven
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.AUGUST, 15)));
        // All Saints' Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.NOVEMBER, 1)));
        // Friday After
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.NOVEMBER, 2)));
        // Armistice Day
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.NOVEMBER, 11)));
        // Monday After
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.NOVEMBER, 12)));
        // Christmas Vacation
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 24)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 25)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 26)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 27)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 28)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 29)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 30)));
        Assert.assertTrue(France.isHoliday(getDate(2018, Calendar.DECEMBER, 31)));
        // Test 2019 Holidays
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.JANUARY, 1)));
        // Good Friday, Easter Monday
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.APRIL, 19)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.APRIL, 22)));
        // May Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.MAY, 1)));
        // Victory in Europe Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.MAY, 8)));
        // Ascension Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.MAY, 30)));
        // Friday After
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.MAY, 31)));
        // Whit Monday
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.JUNE, 10)));
        // Bastille Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.JULY, 14)));
        // Monday After
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.JULY, 15)));
        // Assumption of Mary to Heaven
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.AUGUST, 15)));
        // Friday After
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.AUGUST, 16)));
        // All Saints' Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.NOVEMBER, 1)));
        // Armistice Day
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.NOVEMBER, 11)));
        // Christmas Vacation
        // Monday 23rd
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 23)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 24)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 25)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 26)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 27)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 28)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 29)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 30)));
        Assert.assertTrue(France.isHoliday(getDate(2019, Calendar.DECEMBER, 31)));
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
        HolidayCalendar France = new France();
        // 2018-06-13 13:45:49 GMT (True)
        Assert.assertTrue(France.isWorkingHours(Instant.ofEpochSecond(1528897555)));
        // 2018-07-13 16:00:00 GMT (False)
        assertFalse(France.isWorkingHours(Instant.ofEpochSecond(1531494000)));
        assertTrue(France.isWorkingHours(Instant.ofEpochSecond(1531493999)));
        // 1531465200
        assertFalse(France.isWorkingHours(Instant.ofEpochSecond(1531465199)));
        assertTrue(France.isWorkingHours(Instant.ofEpochSecond(1531465200)));
    }
}