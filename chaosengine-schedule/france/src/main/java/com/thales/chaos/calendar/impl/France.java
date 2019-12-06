/*
 *    Copyright (c) 2019 Thales Group
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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Component("FRA")
public class France extends ChaosCalendar {
    France () {
        super("Europe/Paris", 9, 17);
    }

    @Override
    protected Set<Integer> computeHolidays (int year) {
        Set<Integer> holidays = new HashSet<>();
        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getMovingHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));
        return holidays;
    }

    @Override
    protected Set<Integer> getStaticHolidays (int year) {
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

    @Override
    protected Set<Integer> getMovingHolidays (int year) {
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
}

