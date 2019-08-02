package com.thales.chaos.calendar.impl;

import com.thales.chaos.calendar.ChaosCalendar;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("CZE")
public class Czechia extends ChaosCalendar {
    Czechia () {
        super("Europe/Prague", 9, 17);
    }

    @Override
    protected Collection<Integer> computeHolidays (int year) {
        Set<Integer> holidays = new HashSet<>();
        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));
        return holidays;
    }

    @Override
    protected Set<Integer> getStaticHolidays (int year) {
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

}

