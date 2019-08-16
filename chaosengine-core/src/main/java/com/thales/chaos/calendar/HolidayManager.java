package com.thales.chaos.calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.thales.chaos.util.CalendarUtils.incrementCalendarToMidnight;

@Component
public class HolidayManager {
    private static final Logger log = LoggerFactory.getLogger(HolidayManager.class);
    @Resource(name = "${holidays:CAN}")
    private HolidayCalendar holidayCalendar;

    @Autowired
    HolidayManager () {
    }

    HolidayManager (HolidayCalendar holidayCalendar) {
        this.holidayCalendar = holidayCalendar;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void logCreation () {
        if (log.isInfoEnabled())
            log.info("Holiday Manager is using holidays from {}", holidayCalendar.getClass().getSimpleName());
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
        return getMillisLeftInDay(holidayCalendar.getToday());
    }

    public Instant getInstantAfterWorkingMillis (Instant start, long workingMillis) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(start, holidayCalendar.getTimeZoneId()));
        long millisLeftInDay;
        boolean looper = true;
        do {
            millisLeftInDay = getMillisLeftInDay(calendar);
            if (millisLeftInDay < workingMillis) {
                incrementCalendarToMidnight(calendar);
                workingMillis -= millisLeftInDay;
            } else {
                looper = false;
            }
        } while (looper);
        return calendar.toInstant().plusMillis(workingMillis);
    }

    private long getMillisLeftInDay (Calendar from) {
        if (isHoliday(from) || isWeekend(from)) {
            return 0;
        }
        if (isBeforeStartOfWork(from)) {
            from.set(Calendar.HOUR_OF_DAY, holidayCalendar.getStartOfDay());
        }
        Calendar endOfDay = (Calendar) from.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, holidayCalendar.getEndOfDay());
        endOfDay.set(Calendar.MINUTE, 0);
        endOfDay.set(Calendar.MILLISECOND, 0);
        Instant end = endOfDay.toInstant().truncatedTo(ChronoUnit.HOURS);
        if (from.toInstant().isAfter(end)) {
            return 0;
        }
        return end.toEpochMilli() - from.toInstant().truncatedTo(ChronoUnit.HOURS).toEpochMilli();
    }

    private boolean isWeekend (Calendar from) {
        return holidayCalendar.isWeekend(from);
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

    private boolean isBeforeStartOfWork (Calendar from) {
        return holidayCalendar.isBeforeWorkingHours(from);
    }
}
