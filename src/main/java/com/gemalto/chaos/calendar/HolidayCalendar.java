package com.gemalto.chaos.calendar;

import java.util.Date;
import java.util.List;

public interface HolidayCalendar {

    List<Date> getHolidays(int year);

    boolean isHoliday(Date day);
}
