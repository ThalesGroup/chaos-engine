package com.gemalto.chaos.calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Calendar;

@Component
public class HolidayManager {
    @Resource(name = "${holidays:CAN}")
    private HolidayCalendar holidayCalendar;

    @Autowired
    HolidayManager () {
    }

    HolidayManager (HolidayCalendar holidayCalendar) {
        this.holidayCalendar = holidayCalendar;
    }

    public boolean isHoliday () {
        return isHoliday(holidayCalendar.getToday());
    }

    private boolean isHoliday (Calendar day) {
        return holidayCalendar.isHoliday(day);
    }

    public boolean isWorkingHours () {
        return isWorkingHours(holidayCalendar.getCurrentTime());
    }

    private boolean isWorkingHours (Instant now) {
        return holidayCalendar.isWorkingHours(now);
    }

    public Instant getPreviousWorkingDay () {
        return getPreviousWorkingDay(holidayCalendar.getToday());
    }

    Instant getPreviousWorkingDay (Calendar day) {
        do {
            day.add(Calendar.DATE, -1);
        }
        while (isHoliday(day) || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
        return day.toInstant();
    }
}
