package com.gemalto.chaos.util;

import java.util.Calendar;
import java.util.regex.Pattern;

public class CalendarUtils {
    public static final Pattern datePattern = Pattern.compile(".*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{0,3}Z).*");
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
