package com.gemalto.chaos.calendar;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;

@Component
public class HolidayManager {

    @Resource(name = "${holidays:CAN}")
    private HolidayCalendar holidayCalendar;


    public boolean isHoliday() {
        return isHoliday(Calendar.getInstance());
    }

    private boolean isHoliday(Calendar day) {
        return holidayCalendar.isHoliday(day);
    }


}
