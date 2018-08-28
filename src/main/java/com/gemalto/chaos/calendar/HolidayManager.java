package com.gemalto.chaos.calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

    public Instant getPreviousWorkingDay () {
        return getPreviousWorkingDay(holidayCalendar.getToday());
    }

    Instant getPreviousWorkingDay (Calendar day) {
        shiftBackToLastWorkingDay(day);
        return day.toInstant();
    }

    private void shiftBackToLastWorkingDay (Calendar day) {
        do {
            day.add(Calendar.DATE, -1);
        }
        while (isHoliday(day) || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
    }

    public boolean isHoliday () {
        return isHoliday(holidayCalendar.getToday());
    }

    public long getMillisLeftInDay () {
        if (isHoliday() || isOutsideWorkingHours()) {
            return 0;
        }
        Calendar endOfDay = holidayCalendar.getToday();
        endOfDay.set(Calendar.HOUR_OF_DAY, holidayCalendar.getEndOfDay());
        Instant end = endOfDay.toInstant().truncatedTo(ChronoUnit.HOURS);
        if (Instant.now().isAfter(end)) {
            return 0;
        }
        return (Instant.now().toEpochMilli() - end.toEpochMilli());
    }

    private boolean isHoliday (Calendar day) {
        return holidayCalendar.isHoliday(day);
    }

    public boolean isOutsideWorkingHours () {
        return !isWorkingHours(holidayCalendar.getCurrentTime());
    }

    private boolean isWorkingHours (Instant now) {
        return holidayCalendar.isWorkingHours(now);
    }

    public long getOvernightMillis () {
        Instant startOfDay = getStartOfDay();
        Calendar lastWorkingDay = holidayCalendar.getToday();
        shiftBackToLastWorkingDay(lastWorkingDay);
        lastWorkingDay.set(Calendar.HOUR_OF_DAY, holidayCalendar.getEndOfDay());
        lastWorkingDay.set(Calendar.MINUTE, 0);
        return Duration.between(startOfDay, lastWorkingDay.toInstant()).toMillis();
    }

    public Instant getStartOfDay () {
        Calendar startOfDay = holidayCalendar.getToday();
        startOfDay.set(Calendar.HOUR_OF_DAY, holidayCalendar.getStartOfDay());
        return startOfDay.toInstant().truncatedTo(ChronoUnit.HOURS);
    }

    public long getWorkingMillisInDuration (Duration duration) {
        Instant startTime = Instant.now().minus(duration);
        return getWorkingMillisSinceInstant(startTime);
    }

    public long getWorkingMillisSinceInstant (Instant startTime) {
        Calendar startDay = GregorianCalendar.from(ZonedDateTime.ofInstant(startTime, holidayCalendar.getTimeZoneId()));
        long millis = 0;
        while (startDay.before(getStartOfDay())) {
            millis += isHoliday(startDay) ? getTotalMillisInDay() : 0;
            startDay.add(Calendar.DATE, 1);
        }
        return millis;
    }

    public long getTotalMillisInDay () {
        if (isHoliday()) {
            return 0;
        }
        long startOfDayEpoch = getStartOfDay().truncatedTo(ChronoUnit.HOURS).toEpochMilli();
        Calendar endOfDay = holidayCalendar.getToday();
        endOfDay.set(Calendar.HOUR_OF_DAY, holidayCalendar.getEndOfDay());
        long endOfDayEpoch = endOfDay.toInstant().truncatedTo(ChronoUnit.HOURS).toEpochMilli();
        return endOfDayEpoch - startOfDayEpoch;
    }
}
