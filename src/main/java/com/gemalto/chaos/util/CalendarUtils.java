package com.gemalto.chaos.util;

import java.util.Calendar;

public class CalendarUtils {
    private CalendarUtils () {
    }

    public static void incrementCalendarToMidnight (Calendar calendar) {
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
