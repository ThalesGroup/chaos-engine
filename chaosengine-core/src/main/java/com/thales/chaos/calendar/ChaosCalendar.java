package com.thales.chaos.calendar;

import java.time.ZoneId;
import java.util.*;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;

public abstract class ChaosCalendar implements HolidayCalendar {
    private final Map<Integer, Collection<Integer>> holidayMap = new HashMap<>();
    private final ZoneId timeZoneId;
    private final int startOfDay;
    private final int endOfDay;

    public ChaosCalendar (String timezoneName, int startOfDay, int endOfDay) {
        this.startOfDay = startOfDay;
        this.endOfDay = endOfDay;
        timeZoneId = ZoneId.of(timezoneName);
    }

    @Override
    public boolean isHoliday (Calendar day) {
        int year = day.get(YEAR);
        return holidayMap.computeIfAbsent(year, this::computeHolidays).contains(day.get(DAY_OF_YEAR));
    }

    /**
     * Given a year, calculates all holidays and observed holidays, as well as likely vacation days for the year.
     *
     * @param year The year for which to calculate holidays
     * @return A set of Integers indicating the day of year on which a Holiday will result in a day away from work
     */
    protected Collection<Integer> computeHolidays (int year) {
        Set<Integer> holidays = new TreeSet<>();
        holidays.addAll(getStaticHolidays(year));
        holidays.addAll(getMovingHolidays(year));
        holidays.addAll(getLinkedDays(holidays, year));
        return holidays;
    }

    /**
     * Given a year, returns a set of integers representing the day-of-year of all holidays on specific days.
     *
     * @param year The year for which static holidays should be calculated
     * @return A set of Integers indicating the day of year on which a Holiday will result in a day away from work
     */
    protected Set<Integer> getStaticHolidays (int year) {
        return Collections.emptySet();
    }

    /**
     * Given a year, returns a set of integers representing the day-of-year of all holidays that are on dynamic days
     *
     * @param year The year for which dynamic holidays should be calculated
     * @return A set of Integers indicating the day of year on which a Holiday will result in a day away from work
     */
    protected Set<Integer> getMovingHolidays (int year) {
        return Collections.emptySet();
    }

    @Override
    public ZoneId getTimeZoneId () {
        return timeZoneId;
    }

    @Override
    public int getStartOfDay () {
        return startOfDay;
    }

    @Override
    public int getEndOfDay () {
        return endOfDay;
    }

    @Override
    public TimeZone getTimeZone () {
        return TimeZone.getTimeZone(timeZoneId);
    }
}
