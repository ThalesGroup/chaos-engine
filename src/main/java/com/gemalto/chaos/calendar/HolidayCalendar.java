package com.gemalto.chaos.calendar;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

public interface HolidayCalendar {

    boolean isHoliday(Calendar day);

    default int getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    default int getDate(int year, int month, int weekOfmonth, int dayOfWeek) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        c.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfmonth);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.YEAR, year);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    default Set<Integer> getLinkedDays(Set<Integer> holidays, int year) {
        Set<Integer> linkedDays = new TreeSet<>();
        for (Integer holiday : holidays) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_YEAR, holiday);
            if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                linkedDays.add(c.get(Calendar.DAY_OF_YEAR) + 1);
            } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                linkedDays.add(c.get(Calendar.DAY_OF_YEAR) - 1);
            }
            /*
            We can assume that if a holiday falls on a Tuesday or Thursday,
            people are likely to take the Monday/Tuesday off as well
             */
            else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
                linkedDays.add(c.get(Calendar.DAY_OF_YEAR) - 1);
            } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
                linkedDays.add(c.get(Calendar.DAY_OF_YEAR) + 1);
            }

        }

        return linkedDays;
    }

    default Integer getEaster(int year) {
        // Computus calculation from https://en.wikipedia.org/wiki/Computus
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int n = (h + l - 7 * m + 114) / 31;
        int p = (h + l - 7 * m + 114) % 31;
        Calendar calendar = Calendar.getInstance();
        calendar.clear();


        calendar.set(year, n - 1, p + 1);
        return calendar.get(Calendar.DAY_OF_YEAR);
    }
}
