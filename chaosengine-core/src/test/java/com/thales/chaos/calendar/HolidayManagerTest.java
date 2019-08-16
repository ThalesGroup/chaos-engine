package com.thales.chaos.calendar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class HolidayManagerTest {
    @MockBean(name = "CAN")
    private HolidayCalendar holidayCalendar;
    @Autowired
    private HolidayManager holidayManager;

    @Before
    public void setUp () {
        doCallRealMethod().when(holidayCalendar).isWeekend(any());
        doCallRealMethod().when(holidayCalendar).getToday();
        doCallRealMethod().when(holidayCalendar).isBeforeWorkingHours(any());
        doReturn(false).when(holidayCalendar).isHoliday(any());
        doReturn(ZoneId.of("GMT")).when(holidayCalendar).getTimeZoneId();
        doReturn(8).when(holidayCalendar).getStartOfDay();
        doReturn(16).when(holidayCalendar).getEndOfDay();
    }

    @Test
    public void getPreviousWorkingDay () {
        doReturn(true, false).when(holidayCalendar).isHoliday(any());

        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 29);
        c.set(Calendar.MONTH, Calendar.JUNE);
        c.set(Calendar.YEAR, 2018);
        Instant expected = c.toInstant().truncatedTo(ChronoUnit.DAYS);
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.DAY_OF_MONTH, 3);
        actual.set(Calendar.MONTH, Calendar.JULY);
        actual.set(Calendar.YEAR, 2018);
        Instant previousWorkingDay = holidayManager.getPreviousWorkingDay(actual).truncatedTo(ChronoUnit.DAYS);
        assertEquals(expected, previousWorkingDay);
    }

    @Test
    public void getInstanceAfterWorkingMillis () {
        final Instant startTime = Instant.ofEpochSecond(1565971200);
        assertEquals(Instant.ofEpochSecond(1566204300), holidayManager.getInstantAfterWorkingMillis(startTime, 2700000L));
        assertEquals(Instant.ofEpochSecond(1566313200), holidayManager.getInstantAfterWorkingMillis(startTime, 54000000L));
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private HolidayCalendar holidayCalendar;

        @Bean
        public HolidayManager holidayManager () {
            return new HolidayManager();
        }
    }
}