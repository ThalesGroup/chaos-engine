package com.thales.chaos.calendar.impl;

import com.thales.chaos.calendar.HolidayCalendar;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Calendar;

@Repository("NOP")
public class Nop implements HolidayCalendar {
    @Override
    public boolean isHoliday (Calendar day) {
        return true;
    }

    @Override
    public boolean isWorkingHours (Instant time) {
        return false;
    }

    @Override
    public int getStartOfDay () {
        return 0;
    }

    @Override
    public int getEndOfDay () {
        return 24;
    }

    @Override
    public boolean isBeforeWorkingHours (Calendar from) {
        return true;
    }

    @Override
    public boolean isWeekend (Calendar from) {
        return true;
    }
}
