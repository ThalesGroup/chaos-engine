package com.gemalto.chaos.calendar;


import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Component
public class CanadianHolidays implements HolidayCalendar {

    private static List<Integer> years;
    private static List<Date> holidays;

    public CanadianHolidays() {

    }

    @Override
    public List<Date> getHolidays(int year) {

        List<Date> newHolidays = new ArrayList<>();

        return newHolidays;
    }

    @Override
    public boolean isHoliday(Date day) {
        Integer thisYear = Year.now().getValue();
        if (!years.contains(thisYear)) {
            holidays.addAll(getHolidays(thisYear));
            years.add(thisYear);
        }
        return holidays.contains(day);
    }
}
