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

import com.thales.chaos.calendar.HolidayCalendar;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import static java.util.Calendar.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

@RunWith(Enclosed.class)
public class USATest {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'of' LLLL yyyy");
    private static final String NEW_YEARS = "New years";
    private static final String MARTIN_LUTHER_KING_JR_DAY = "Martin Luther King Jr. Day";
    private static final String PRESIDENTS_DAY = "Presidents' Day";
    private static final String MEMORIAL_DAY = "Memorial Day";
    private static final String INDEPENDENCE_DAY = "Independence Day";
    private static final String LABOUR_DAY = "Labour Day";
    private static final String COLUMBUS_DAY = "Columbus Day";
    private static final String VETERANS_DAY = "Veterans Day";
    private static final String THANKSGIVING = "Thanksgiving";
    private static final String CHRISTMAS_HOLIDAYS = "Christmas Holidays";
    private static final String OBSERVED = " (observed)";

    private static Calendar getDate (int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c;
    }

    @RunWith(Parameterized.class)
    public static class Holidays {
        private static final String LINKED_FRIDAY = " (linked Friday)";
        private final HolidayCalendar usa = spy(new USA());
        private Calendar holiday;
        private String description;

        public Holidays (int year, int month, int day, String description) {
            this.holiday = getDate(year, month, day);
            this.description = description;
        }

        @Parameterized.Parameters(name = "{0} {3}")
        public static Collection<Object[]> parameters () {
            Collection<Object[]> holidays = new ArrayList<>();
            // 2018
            holidays.add(new Object[]{ 2018, JANUARY, 1, NEW_YEARS });
            holidays.add(new Object[]{ 2018, JANUARY, 15, MARTIN_LUTHER_KING_JR_DAY });
            holidays.add(new Object[]{ 2018, FEBRUARY, 19, PRESIDENTS_DAY });
            holidays.add(new Object[]{ 2018, MAY, 28, MEMORIAL_DAY });
            holidays.add(new Object[]{ 2018, JULY, 4, INDEPENDENCE_DAY });
            holidays.add(new Object[]{ 2018, SEPTEMBER, 3, LABOUR_DAY });
            holidays.add(new Object[]{ 2018, OCTOBER, 8, COLUMBUS_DAY });
            holidays.add(new Object[]{ 2018, NOVEMBER, 11, VETERANS_DAY });
            holidays.add(new Object[]{ 2018, NOVEMBER, 12, VETERANS_DAY + OBSERVED });
            holidays.add(new Object[]{ 2018, NOVEMBER, 22, THANKSGIVING });
            holidays.add(new Object[]{ 2018, NOVEMBER, 23, THANKSGIVING + LINKED_FRIDAY });
            for (int i = 24; i < 32; i++) {
                holidays.add(new Object[]{ 2018, DECEMBER, i, CHRISTMAS_HOLIDAYS + " (" + i + ")" });
            }
            // 2019
            holidays.add(new Object[]{ 2019, JANUARY, 1, NEW_YEARS });
            holidays.add(new Object[]{ 2019, JANUARY, 21, MARTIN_LUTHER_KING_JR_DAY });
            holidays.add(new Object[]{ 2019, FEBRUARY, 18, PRESIDENTS_DAY });
            holidays.add(new Object[]{ 2019, MAY, 27, MEMORIAL_DAY });
            holidays.add(new Object[]{ 2019, JULY, 4, INDEPENDENCE_DAY });
            holidays.add(new Object[]{ 2019, JULY, 5, INDEPENDENCE_DAY + LINKED_FRIDAY });
            holidays.add(new Object[]{ 2019, SEPTEMBER, 2, LABOUR_DAY });
            holidays.add(new Object[]{ 2019, OCTOBER, 14, COLUMBUS_DAY });
            holidays.add(new Object[]{ 2019, NOVEMBER, 11, VETERANS_DAY });
            holidays.add(new Object[]{ 2019, NOVEMBER, 28, THANKSGIVING });
            holidays.add(new Object[]{ 2019, NOVEMBER, 29, THANKSGIVING + LINKED_FRIDAY });
            for (int i = 23; i < 32; i++) {
                holidays.add(new Object[]{ 2019, DECEMBER, i, CHRISTMAS_HOLIDAYS + " (" + i + ")" });
            }
            // 2020
            holidays.add(new Object[]{ 2020, JANUARY, 1, NEW_YEARS });
            holidays.add(new Object[]{ 2020, JANUARY, 20, MARTIN_LUTHER_KING_JR_DAY });
            holidays.add(new Object[]{ 2020, FEBRUARY, 17, PRESIDENTS_DAY });
            holidays.add(new Object[]{ 2020, MAY, 25, MEMORIAL_DAY });
            holidays.add(new Object[]{ 2020, JULY, 3, INDEPENDENCE_DAY + OBSERVED });
            holidays.add(new Object[]{ 2020, JULY, 4, INDEPENDENCE_DAY });
            holidays.add(new Object[]{ 2020, SEPTEMBER, 7, LABOUR_DAY });
            holidays.add(new Object[]{ 2020, OCTOBER, 12, COLUMBUS_DAY });
            holidays.add(new Object[]{ 2020, NOVEMBER, 11, VETERANS_DAY });
            holidays.add(new Object[]{ 2020, NOVEMBER, 26, THANKSGIVING });
            holidays.add(new Object[]{ 2020, NOVEMBER, 27, THANKSGIVING + LINKED_FRIDAY });
            for (int i = 24; i < 32; i++) {
                holidays.add(new Object[]{ 2020, DECEMBER, i, CHRISTMAS_HOLIDAYS + " (" + i + ")" });
            }
            // 2021
            holidays.add(new Object[]{ 2021, JANUARY, 1, NEW_YEARS });
            holidays.add(new Object[]{ 2021, JANUARY, 18, MARTIN_LUTHER_KING_JR_DAY });
            holidays.add(new Object[]{ 2021, FEBRUARY, 15, PRESIDENTS_DAY });
            holidays.add(new Object[]{ 2021, MAY, 31, MEMORIAL_DAY });
            holidays.add(new Object[]{ 2021, JULY, 4, INDEPENDENCE_DAY });
            holidays.add(new Object[]{ 2021, JULY, 5, INDEPENDENCE_DAY + OBSERVED });
            holidays.add(new Object[]{ 2021, SEPTEMBER, 6, LABOUR_DAY });
            holidays.add(new Object[]{ 2021, OCTOBER, 11, COLUMBUS_DAY });
            holidays.add(new Object[]{ 2021, NOVEMBER, 11, VETERANS_DAY });
            holidays.add(new Object[]{ 2021, NOVEMBER, 25, THANKSGIVING });
            holidays.add(new Object[]{ 2021, NOVEMBER, 26, THANKSGIVING + LINKED_FRIDAY });
            for (int i = 24; i < 32; i++) {
                holidays.add(new Object[]{ 2021, DECEMBER, i, CHRISTMAS_HOLIDAYS + " (" + i + ")" });
            }
            return holidays;
        }

        @Test
        public void isHoliday () {
            assertTrue(errorMessage(), usa.isHoliday(holiday));
        }

        private String errorMessage () {
            final ZonedDateTime zonedDateTime = holiday.toInstant().atZone(usa.getTimeZoneId());
            return description + " on " + zonedDateTime.format(dateTimeFormatter) + " should be a holiday";
        }
    }

    @RunWith(Parameterized.class)
    public static class WorkingDays {
        private static final String DAY_AFTER = "Day after ";
        private static final String DAY_BEFORE = "Day before ";
        private static final String LAST_FULL_WORKING_DAY_BEFORE_CHRISTMAS = "Last full working Day before Christmas";
        private static final String GOOD_FRIDAY = "Good Friday";
        private final HolidayCalendar usa = spy(new USA());
        private Calendar workingDay;
        private String description;

        public WorkingDays (int year, int month, int day, String description) {
            this.workingDay = getDate(year, month, day);
            this.description = description;
        }

        @Parameterized.Parameters(name = "{0} {3}")
        public static Collection<Object[]> parameters () {
            Collection<Object[]> workingDays = new ArrayList<>();
            // 2018
            workingDays.add(new Object[]{ 2018, JANUARY, 2, DAY_AFTER + NEW_YEARS });
            workingDays.add(new Object[]{ 2018, JANUARY, 16, DAY_AFTER + MARTIN_LUTHER_KING_JR_DAY });
            workingDays.add(new Object[]{ 2018, FEBRUARY, 20, DAY_AFTER + PRESIDENTS_DAY });
            workingDays.add(new Object[]{ 2018, MAY, 29, DAY_AFTER + MEMORIAL_DAY });
            workingDays.add(new Object[]{ 2018, JULY, 5, DAY_AFTER + INDEPENDENCE_DAY });
            workingDays.add(new Object[]{ 2018, SEPTEMBER, 4, DAY_AFTER + LABOUR_DAY });
            workingDays.add(new Object[]{ 2018, OCTOBER, 9, DAY_AFTER + COLUMBUS_DAY });
            workingDays.add(new Object[]{ 2018, NOVEMBER, 13, DAY_AFTER + VETERANS_DAY + OBSERVED });
            workingDays.add(new Object[]{ 2018, NOVEMBER, 21, DAY_BEFORE + THANKSGIVING });
            workingDays.add(new Object[]{ 2018, DECEMBER, 23, LAST_FULL_WORKING_DAY_BEFORE_CHRISTMAS });
            // 2019
            workingDays.add(new Object[]{ 2019, JANUARY, 2, DAY_AFTER + NEW_YEARS });
            workingDays.add(new Object[]{ 2019, JANUARY, 22, DAY_AFTER + MARTIN_LUTHER_KING_JR_DAY });
            workingDays.add(new Object[]{ 2019, FEBRUARY, 19, DAY_AFTER + PRESIDENTS_DAY });
            workingDays.add(new Object[]{ 2019, MAY, 28, DAY_AFTER + MEMORIAL_DAY });
            workingDays.add(new Object[]{ 2019, JULY, 3, DAY_BEFORE + INDEPENDENCE_DAY });
            workingDays.add(new Object[]{ 2019, SEPTEMBER, 3, DAY_AFTER + LABOUR_DAY });
            workingDays.add(new Object[]{ 2019, OCTOBER, 15, DAY_AFTER + COLUMBUS_DAY });
            workingDays.add(new Object[]{ 2019, NOVEMBER, 10, DAY_BEFORE + VETERANS_DAY });
            workingDays.add(new Object[]{ 2019, NOVEMBER, 12, DAY_AFTER + VETERANS_DAY });
            workingDays.add(new Object[]{ 2019, NOVEMBER, 27, DAY_BEFORE + THANKSGIVING });
            workingDays.add(new Object[]{ 2019, DECEMBER, 20, LAST_FULL_WORKING_DAY_BEFORE_CHRISTMAS });
            // 2020
            workingDays.add(new Object[]{ 2020, JANUARY, 2, DAY_AFTER + NEW_YEARS });
            workingDays.add(new Object[]{ 2020, JANUARY, 21, DAY_AFTER + MARTIN_LUTHER_KING_JR_DAY });
            workingDays.add(new Object[]{ 2020, FEBRUARY, 18, DAY_AFTER + PRESIDENTS_DAY });
            workingDays.add(new Object[]{ 2020, MAY, 26, DAY_AFTER + MEMORIAL_DAY });
            workingDays.add(new Object[]{ 2020, JULY, 2, DAY_BEFORE + INDEPENDENCE_DAY + OBSERVED });
            workingDays.add(new Object[]{ 2020, JULY, 6, "Monday after " + INDEPENDENCE_DAY });
            workingDays.add(new Object[]{ 2020, SEPTEMBER, 8, DAY_AFTER + LABOUR_DAY });
            workingDays.add(new Object[]{ 2020, OCTOBER, 13, DAY_AFTER + COLUMBUS_DAY });
            workingDays.add(new Object[]{ 2019, NOVEMBER, 10, DAY_BEFORE + VETERANS_DAY });
            workingDays.add(new Object[]{ 2020, NOVEMBER, 12, DAY_AFTER + VETERANS_DAY });
            workingDays.add(new Object[]{ 2020, NOVEMBER, 25, DAY_BEFORE + THANKSGIVING });
            workingDays.add(new Object[]{ 2020, DECEMBER, 23, LAST_FULL_WORKING_DAY_BEFORE_CHRISTMAS });
            // 2021
            workingDays.add(new Object[]{ 2021, JANUARY, 2, DAY_AFTER + NEW_YEARS });
            workingDays.add(new Object[]{ 2021, JANUARY, 19, DAY_AFTER + MARTIN_LUTHER_KING_JR_DAY });
            workingDays.add(new Object[]{ 2021, FEBRUARY, 16, DAY_AFTER + PRESIDENTS_DAY });
            workingDays.add(new Object[]{ 2021, JUNE, 1, DAY_AFTER + MEMORIAL_DAY });
            workingDays.add(new Object[]{ 2021, JULY, 3, DAY_BEFORE + INDEPENDENCE_DAY });
            workingDays.add(new Object[]{ 2021, JULY, 6, DAY_AFTER + INDEPENDENCE_DAY + OBSERVED });
            workingDays.add(new Object[]{ 2021, SEPTEMBER, 7, DAY_AFTER + LABOUR_DAY });
            workingDays.add(new Object[]{ 2021, OCTOBER, 12, DAY_AFTER + COLUMBUS_DAY });
            workingDays.add(new Object[]{ 2021, NOVEMBER, 10, DAY_BEFORE + VETERANS_DAY });
            workingDays.add(new Object[]{ 2021, NOVEMBER, 23, DAY_BEFORE + THANKSGIVING });
            workingDays.add(new Object[]{ 2021, DECEMBER, 23, LAST_FULL_WORKING_DAY_BEFORE_CHRISTMAS });
            // Good Friday is a state holiday in 11 States
            workingDays.add(new Object[]{ 2015, APRIL, 3, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2016, MARCH, 25, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2017, APRIL, 14, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2018, MARCH, 30, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2019, APRIL, 19, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2020, APRIL, 10, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2021, APRIL, 2, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2022, APRIL, 15, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2023, APRIL, 7, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2024, MARCH, 29, GOOD_FRIDAY });
            workingDays.add(new Object[]{ 2025, APRIL, 18, GOOD_FRIDAY });
            return workingDays;
        }

        @Test
        public void isWorkingDay () {
            assertFalse(errorMessage(), usa.isHoliday(workingDay));
        }

        private String errorMessage () {
            final ZonedDateTime zonedDateTime = workingDay.toInstant().atZone(usa.getTimeZoneId());
            return description + " on " + zonedDateTime.format(dateTimeFormatter) + " should be a work day";
        }
    }
}