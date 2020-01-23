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

package com.thales.chaos.calendar.impl;

import com.thales.chaos.calendar.ChaosCalendar;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

@Component("CAN")
public class Canada extends ChaosCalendar {
    Canada () {
        super("America/Toronto", 9, 17);
    }

    @Override
    protected Set<Integer> getStaticHolidays (int year) {
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

    @Override
    protected Set<Integer> getMovingHolidays (int year) {
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

    private static Integer getVictoriaDay (int year) {
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
}

