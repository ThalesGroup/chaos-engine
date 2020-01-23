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
