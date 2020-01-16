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

package com.thales.chaos.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CalendarUtilsTest {
    @Test
    public void testIncrementCalendarToMidnight () throws Exception {
        Calendar calendar = Mockito.mock(Calendar.class);
        CalendarUtils.incrementCalendarToMidnight(calendar);
        verify(calendar).add(eq(Calendar.DAY_OF_YEAR), eq(1));
        verify(calendar).set(eq(Calendar.HOUR_OF_DAY), eq(0));
        verify(calendar).set(eq(Calendar.MINUTE), eq(0));
        verify(calendar).set(eq(Calendar.SECOND), eq(0));
        verify(calendar).set(eq(Calendar.MILLISECOND), eq(0));
    }
}