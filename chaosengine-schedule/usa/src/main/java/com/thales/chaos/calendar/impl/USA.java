package com.thales.chaos.calendar.impl;

import com.thales.chaos.calendar.HolidayCalendar;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;

import static java.util.Calendar.*;

@Component("USA")
public class USA implements HolidayCalendar {
    private static final String TZ = "America/New_York";
    private static final ZoneId TIME_ZONE_ID = ZoneId.of(TZ);
    private static final int START_OF_DAY = 9;
    private static final int END_OF_DAY = 17;
    private final Map<Integer, Collection<Integer>> holidayMap = new HashMap<>();

    @Override
    public boolean isHoliday (Calendar day) {
        int year = day.get(YEAR);
        return holidayMap.computeIfAbsent(year, this::computeHolidays).contains(day.get(DAY_OF_YEAR));
    }

    private Collection<Integer> computeHolidays (int year) {
        Set<Integer> holidays = new TreeSet<>();
        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getMovingHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));
        return holidays;
    }

    private Set<Integer> getStaticHolidays (int year) {
        Set<Integer> staticHolidays = new TreeSet<>();
        // New Years
        staticHolidays.add(getDate(year, JANUARY, 1));
        // Canada Day
        staticHolidays.add(getDate(year, JULY, 4));
        // Veterans Day
        staticHolidays.add(getDate(year, NOVEMBER, 11));
        // Christmas Eve through New Years
        staticHolidays.add(getDate(year, DECEMBER, 24));
        staticHolidays.add(getDate(year, DECEMBER, 25));
        staticHolidays.add(getDate(year, DECEMBER, 26));
        staticHolidays.add(getDate(year, DECEMBER, 27));
        staticHolidays.add(getDate(year, DECEMBER, 28));
        staticHolidays.add(getDate(year, DECEMBER, 29));
        staticHolidays.add(getDate(year, DECEMBER, 30));
        staticHolidays.add(getDate(year, DECEMBER, 31));
        return staticHolidays;
    }

    private Set<Integer> getMovingHolidays (int year) {
        Set<Integer> movingHolidays = new TreeSet<>();
        // Martin Luther King Day, third Monday of January
        movingHolidays.add(getDate(year, JANUARY, 3, MONDAY));
        // Presidents Day, third Monday of February
        movingHolidays.add(getDate(year, FEBRUARY, 3, MONDAY));
        // Memorial Day, last Monday in May
        movingHolidays.add(getDate(year, MAY, -1, MONDAY));
        // Labour Day, first Monday of September.
        movingHolidays.add(getDate(year, SEPTEMBER, 1, MONDAY));
        // Columbus Day, second Monday of October
        movingHolidays.add(getDate(year, OCTOBER, 2, MONDAY));
        // Thanksgiving, fourth Thursday of November
        movingHolidays.add(getDate(year, NOVEMBER, 4, THURSDAY));
        return movingHolidays;
    }

    @Override
    public ZoneId getTimeZoneId () {
        return TIME_ZONE_ID;
    }

    @Override
    public int getStartOfDay () {
        return START_OF_DAY;
    }

    @Override
    public int getEndOfDay () {
        return END_OF_DAY;
    }

    @Override
    public TimeZone getTimeZone () {
        return TimeZone.getTimeZone(TZ);
    }
}
