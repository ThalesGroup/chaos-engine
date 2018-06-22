package com.gemalto.chaos.calendar.impl;

import com.gemalto.chaos.calendar.HolidayCalendar;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

@Repository("CZE")
public class Czechia implements HolidayCalendar {
    private static final String TZ = "Europe/Prague";
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
        holidays.addAll(getLinkedDays(holidays, year));
        holidays.add(year);
    }

    private Set<Integer> getStaticHolidays (int year) {
        Set<Integer> staticHolidays = new TreeSet<>();
        // Restoration Day of the Independent Czech State; New Year's Day
        staticHolidays.add(getDate(year, Calendar.JANUARY, 1));
        //Labour Day
        staticHolidays.add(getDate(year, Calendar.MAY, 1));
        //Liberation Day
        staticHolidays.add(getDate(year, Calendar.MAY, 8));
        //Saints Cyril and Methodius Day
        staticHolidays.add(getDate(year, Calendar.JULY, 5));
        //Jan Hus
        staticHolidays.add(getDate(year, Calendar.JULY, 6));
        //St. Wenceslas Day
        staticHolidays.add(getDate(year, Calendar.SEPTEMBER, 28));
        //Independence Day
        staticHolidays.add(getDate(year, Calendar.OCTOBER, 28));
        //Struggle for Freedom and Democracy Day
        staticHolidays.add(getDate(year, Calendar.NOVEMBER, 17));
        //Christmas Eve
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 24));
        //Christmas Day
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 25));
        //St. Stephen's Day
        staticHolidays.add(getDate(year, Calendar.DECEMBER, 26));
        return staticHolidays;
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
