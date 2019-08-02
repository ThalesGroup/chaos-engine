package com.thales.chaos.calendar.impl;

import com.thales.chaos.calendar.ChaosCalendar;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Calendar.*;

@Component("USA")
public class USA extends ChaosCalendar {
    USA () {
        super("America/New_York", 9, 17);
    }

    @Override
    protected Set<Integer> getStaticHolidays (int year) {
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

    @Override
    protected Set<Integer> getMovingHolidays (int year) {
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

}
