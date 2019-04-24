package com.gemalto.chaos.calendar.impl;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.*;

@Repository("FRA")
public class France implements HolidayCalendar {
    private static final String TZ = "Europe/Paris";
    private static final ZoneId TIME_ZONE_ID = ZoneId.of(TZ);
    private static final int START_OF_DAY = 9;
    private static final int END_OF_DAY = 17;
    private final Set<Integer> holidays = new TreeSet<>();

    @Override
    public boolean isHoliday (Calendar day) {
        int year = day.get(Calendar.YEAR);
        if (!holidays.contains(year)) {
            renderHolidays(year);
        }
        return holidays.contains(day.get(Calendar.DAY_OF_YEAR));
    }

    private void renderHolidays (int year) {
        holidays.clear();
        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getMovingHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));
        holidays.add(year);
    }

    private Collection<? extends Integer> getStaticHolidays (int year) {
        Set<Integer> staticHolidays = new TreeSet<>();
        // New Years
        staticHolidays.add(getDate(year, Calendar.JANUARY, 1));
        // May Day / Labour Day, 1 May
        staticHolidays.add(getDate(year, Calendar.MAY, 1));
        // Victory in Europe Day, 8 May
        staticHolidays.add(getDate(year, Calendar.MAY, 8));
        // Bastille Day, 14 July
        staticHolidays.add(getDate(year, Calendar.JULY, 14));
        // Assumption of Mary to Heaven, 15 August
        staticHolidays.add(getDate(year, Calendar.AUGUST, 15));
        // All Saint's Day, 1 November
        staticHolidays.add(getDate(year, Calendar.NOVEMBER, 1));
        // Armistice Day, 11 November
        staticHolidays.add(getDate(year, Calendar.NOVEMBER, 11));
        // Christmas Eve through New Years
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 24));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 25));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 26));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 27));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 28));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 29));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 30));
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 31));
        return staticHolidays;
    }

    private Collection<? extends Integer> getMovingHolidays (int year) {
        Set<Integer> movingHolidays = new TreeSet<>();
        Integer easter = getEaster(year);
        // Good Friday, 2 days before Easter
        movingHolidays.add(easter - 2);
        // Easter Monday, 1 day after Easter
        movingHolidays.add(easter + 1);
        // Ascension Day, 39 days after Easter
        movingHolidays.add(easter + 39);
        // Whit Monday, 50 days after Easter
        movingHolidays.add(easter + 50);
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

