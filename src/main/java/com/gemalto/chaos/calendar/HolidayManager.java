package com.gemalto.chaos.calendar;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Calendar;

@Component
public class HolidayManager {

    @Resource(name = "${holidays:CAN}")
    private HolidayCalendar holidayCalendar;


    public boolean isHoliday() {
        return isHoliday(holidayCalendar.getToday());
    }

    private boolean isHoliday(Calendar day) {
        return holidayCalendar.isHoliday(day);
    }

    public boolean isWorkingHours() {
        return isWorkingHours(holidayCalendar.getCurrentTime());
    }

    private boolean isWorkingHours(Instant now) {
        return holidayCalendar.isWorkingHours(now);
    }


}
