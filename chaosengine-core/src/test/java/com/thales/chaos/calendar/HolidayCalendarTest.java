package com.thales.chaos.calendar;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Set;

import static java.util.Calendar.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class HolidayCalendarTest {
    private static final String DEFAULT_TIMEZONE = "GMT";
    private HolidayCalendar holidayCalendar;

    @Before
    public void setUp () {
        holidayCalendar = new HolidayCalendar() {
            @Override
            public boolean isHoliday (Calendar day) {
                return false;
            }

            @Override
            public int getStartOfDay () {
                return 9;
            }

            @Override
            public int getEndOfDay () {
                return 16;
            }
        };
    }

    @Test
    public void isWorkingHours () {
        assertFalse(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562414400))); // Saturday, July 6th 2019
        assertFalse(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562500800))); // Sunday, July 7th 2019
        assertFalse(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562576399))); // Monday, July 8th 2019, 9:00:00 - 1ms
        assertTrue(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562576400))); // Monday, July 8th 2019, 9:00:00
        assertTrue(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562601599))); // Monday, July 8th 2019, 16:00:00 - 1ms
        assertFalse(holidayCalendar.isWorkingHours(Instant.ofEpochSecond(1562601600))); // Monday, July 8th 2019, 16:00:00
    }

    @Test
    public void getTimeZoneId () {
        assertEquals(ZoneId.of(DEFAULT_TIMEZONE), holidayCalendar.getTimeZoneId());
    }

    @Test
    public void getDate () {
        assertEquals(1, holidayCalendar.getDate(2019, JANUARY, 1));
        assertEquals(201, holidayCalendar.getDate(2019, JULY, 20));
        assertEquals(202, holidayCalendar.getDate(2019, JULY, 21));
        assertEquals(304, holidayCalendar.getDate(2019, OCTOBER, 31));
        assertEquals(359, holidayCalendar.getDate(2019, DECEMBER, 25));
        assertEquals(365, holidayCalendar.getDate(2019, DECEMBER, 31));
        assertEquals(366, holidayCalendar.getDate(2020, DECEMBER, 31));
    }

    @Test
    public void getDateByWeek () {
        assertEquals(185, holidayCalendar.getDate(2016, JULY, 1, 1));
        assertEquals(183, holidayCalendar.getDate(2017, JULY, 1, 1));
        assertEquals(182, holidayCalendar.getDate(2018, JULY, 1, 1));
        assertEquals(188, holidayCalendar.getDate(2019, JULY, 1, 1));
        assertEquals(187, holidayCalendar.getDate(2020, JULY, 1, 1));
    }

    @Test
    public void getLinkedDays () {
        final int year = 2019;
        int sundayHoliday = holidayCalendar.getDate(year, FEBRUARY, 2, SUNDAY);
        int saturdayHoliday = holidayCalendar.getDate(year, MARCH, 2, SATURDAY);
        int tuesdayHoliday = holidayCalendar.getDate(year, APRIL, 2, TUESDAY);
        int thursdayHoliday = holidayCalendar.getDate(year, MAY, 2, THURSDAY);
        final Set<Integer> linkedDays = holidayCalendar.getLinkedDays(Set.of(sundayHoliday, saturdayHoliday, tuesdayHoliday, thursdayHoliday), year);
        assertThat(linkedDays, containsInAnyOrder(sundayHoliday + 1, saturdayHoliday - 1, tuesdayHoliday - 1, thursdayHoliday + 1));
    }

    @Test
    public void getEaster () {
        assertEquals(87, holidayCalendar.getEaster(2016));
        assertEquals(106, holidayCalendar.getEaster(2017));
        assertEquals(91, holidayCalendar.getEaster(2018));
        assertEquals(111, holidayCalendar.getEaster(2019));
        assertEquals(103, holidayCalendar.getEaster(2020));
        assertEquals(94, holidayCalendar.getEaster(2021));
        assertEquals(107, holidayCalendar.getEaster(2022));
        assertEquals(99, holidayCalendar.getEaster(2023));
        assertEquals(91, holidayCalendar.getEaster(2024));
        assertEquals(110, holidayCalendar.getEaster(2025));
        assertEquals(95, holidayCalendar.getEaster(2026));
        assertEquals(87, holidayCalendar.getEaster(2027));
        assertEquals(107, holidayCalendar.getEaster(2028));
        assertEquals(91, holidayCalendar.getEaster(2029));
        assertEquals(111, holidayCalendar.getEaster(2030));
        assertEquals(103, holidayCalendar.getEaster(2031));
    }
}