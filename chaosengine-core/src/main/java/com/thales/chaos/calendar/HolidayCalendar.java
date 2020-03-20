/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.calendar;

import java.time.*;
import java.util.*;

public interface HolidayCalendar {
    boolean isHoliday (Calendar day);

    default boolean isWorkingHours (Instant time) {
        ZonedDateTime zdt = time.atZone(getTimeZoneId());
        DayOfWeek dow = zdt.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY || dow == DayOfWeek.SATURDAY) {
            return false;
        }
        int hour = zdt.getHour();
        return hour >= getStartOfDay() && hour < getEndOfDay();
    }

    default ZoneId getTimeZoneId () {
        return ZoneId.of("GMT");
    }

    int getStartOfDay ();

    int getEndOfDay ();

    default Instant getCurrentTime () {
        return Instant.now(Clock.system(getTimeZoneId()));
    }

    default Calendar getToday () {
        return new GregorianCalendar(getTimeZone());
    }

    default TimeZone getTimeZone () {
        return TimeZone.getTimeZone("GMT");
    }

    /**
     * Given a year, month, and day, return an integer day-of-year
     *
     * @param year  Given year
     * @param month Given month
     * @param day   Given day-of-month
     * @return Integer day-of-year value matching the parameters
     */
    default int getDate (int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Given a year, month, week of month, and day of week, return the integer day-of-year
     *
     * @param year Given year
     * @param month Given month
     * @param weekOfmonth Given Week-of-Month
     * @param dayOfWeek Given day-of-week
     * @return Integer day-of-year value matching the parameters
     */
    default int getDate (int year, int month, int weekOfmonth, int dayOfWeek) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        c.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfmonth);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.YEAR, year);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Calculates days related to known holidays that are either observed days for work, or isolated days in the workweek which many people will take as a vacation day.
     *
     * @param holidays A set of Integers representing days of the year on which a holiday occurs
     * @param year     The year associated with the set holidays set
     * @return A set of Integers representing days of the year where many or all people will be out of the office due to holidays
     */
    default Set<Integer> getLinkedDays (Set<Integer> holidays, int year) {
        Set<Integer> linkedDays = new TreeSet<>();
        for (Integer holiday : holidays) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
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

    @SuppressWarnings("MagicConstant")
    // Easter Algorithm depends on math, and therefore, calendar is set with n-1 instead of Calendar.MARCH or Calendar.APRIL
    default int getEaster (int year) {
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

    default boolean isBeforeWorkingHours (Calendar from) {
        Instant time = from.toInstant();
        ZonedDateTime zdt = time.atZone(getTimeZoneId());
        DayOfWeek dow = zdt.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY || dow == DayOfWeek.SATURDAY) {
            return false;
        }
        int hour = zdt.getHour();
        return hour < getStartOfDay();
    }

    default boolean isWeekend (Calendar from) {
        int dayOfWeek = from.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY;
    }
}

