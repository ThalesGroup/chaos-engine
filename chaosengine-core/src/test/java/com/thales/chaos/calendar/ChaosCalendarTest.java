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

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(Parameterized.class)
public class ChaosCalendarTest {
    private static final int YEAR = Calendar.getInstance().get(Calendar.YEAR);
    private final String timeZone;
    private final int startOfDay;
    private final int endOfDay;
    private ChaosCalendar chaosCalendar;

    public ChaosCalendarTest (String timeZone, int startOfDay, int endOfDay) {
        this.timeZone = timeZone;
        this.startOfDay = startOfDay;
        this.endOfDay = endOfDay;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters () {
        return List.of(new Object[]{ "GMT", 8, 15 }, new Object[]{ "CET", 9, 16 }, new Object[]{ "America/Toronto", 8, 17 });
    }

    @Before
    public void setUp () {
        chaosCalendar = spy(new ChaosCalendar(timeZone, startOfDay, endOfDay) {
        });
    }

    @Test
    public void isHoliday () {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        doReturn(Collections.emptySet()).when(chaosCalendar).computeHolidays(year);
        verify(chaosCalendar, never()).computeHolidays(year);
        assertFalse(chaosCalendar.isHoliday(calendar));
        verify(chaosCalendar, times(1)).computeHolidays(year);
        assertFalse(chaosCalendar.isHoliday(calendar));
        verify(chaosCalendar, times(1)).computeHolidays(year);
    }

    @Test
    public void computeHolidays () {
        doReturn(Set.of(1, 5, 7)).when(chaosCalendar).getStaticHolidays(YEAR);
        doReturn(Set.of(50, 80, 90)).when(chaosCalendar).getMovingHolidays(YEAR);
        doReturn(Set.of(2, 51, 91)).when(chaosCalendar).getLinkedDays(Set.of(1, 5, 7, 50, 80, 90), YEAR);
        assertThat(chaosCalendar.computeHolidays(YEAR), containsInAnyOrder(1, 2, 5, 7, 50, 51, 80, 90, 91));
    }

    @Test
    public void getStaticHolidaysDefault () {
        assertThat(chaosCalendar.getStaticHolidays(YEAR), IsEmptyCollection.empty());
    }

    @Test
    public void getMovingHolidaysDefault () {
        assertThat(chaosCalendar.getMovingHolidays(YEAR), IsEmptyCollection.empty());
    }

    @Test
    public void getTimeZoneId () {
        assertEquals(ZoneId.of(timeZone), chaosCalendar.getTimeZoneId());
    }

    @Test
    public void getStartOfDay () {
        assertEquals(startOfDay, chaosCalendar.getStartOfDay());
    }

    @Test
    public void getEndOfDay () {
        assertEquals(endOfDay, chaosCalendar.getEndOfDay());
    }
}