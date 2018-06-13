package com.gemalto.chaos.calendar.holidaycalendar;


import com.gemalto.chaos.calendar.HolidayCalendar;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;


@Repository("CAN")
public class Canada implements HolidayCalendar {

    private final Set<Integer> holidays = new TreeSet<>();

    private static Integer getVictoriaDay(int year) {
        // Victoria is the Monday before May 25th
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Calendar.MAY);
        c.set(Calendar.DAY_OF_MONTH, 25);
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            c.add(Calendar.DATE, -1);
        }
        return c.get(Calendar.DAY_OF_YEAR);
    }

    private void renderHolidays(int year) {
        holidays.clear();

        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getMovingHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));

        holidays.add(year);
    }

    private Set<Integer> getStaticHolidays(int year) {
        Set<Integer> staticHolidays = new TreeSet<>();

        // New Years
        staticHolidays.add(getDate(year, Calendar.JANUARY, 1));

        // Canada Day
        staticHolidays.add(getDate(year, Calendar.JULY, 1));

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

    private Set<Integer> getMovingHolidays(int year) {
        Set<Integer> movingHolidays = new TreeSet<>();

        // Family Day, third Monday of February
        movingHolidays.add(getDate(year, Calendar.FEBRUARY, 3, Calendar.MONDAY));

        // Good Friday, 2 days before Easter
        movingHolidays.add(getEaster(year) - 2);

        // Victoria Day
        movingHolidays.add(getVictoriaDay(year));

        // Civic Holiday, first Monday of August.
        movingHolidays.add(getDate(year, Calendar.AUGUST, 1, Calendar.MONDAY));

        // Labour Day, first Monday of September.
        movingHolidays.add(getDate(year, Calendar.SEPTEMBER, 1, Calendar.MONDAY));

        // Thanksgiving, second Monday of October
        movingHolidays.add(getDate(year, Calendar.OCTOBER, 2, Calendar.MONDAY));

        return movingHolidays;
    }

    @Override
    public boolean isHoliday(Calendar day) {
        int year = day.get(Calendar.YEAR);
        if (!holidays.contains(year)) {
            renderHolidays(year);
        }
        return holidays.contains(day.get(Calendar.DAY_OF_YEAR));
    }
}
