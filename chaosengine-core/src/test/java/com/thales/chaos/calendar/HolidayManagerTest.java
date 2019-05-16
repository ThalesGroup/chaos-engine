package com.thales.chaos.calendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class HolidayManagerTest {
    @MockBean(name = "CAN")
    private HolidayCalendar holidayCalendar;
    @Autowired
    private HolidayManager holidayManager;

    @Before
    public void setUp () {
        doReturn(true, false).when(holidayCalendar).isHoliday(any());
    }

    @Test
    public void getPreviousWorkingDay () {
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
        Assert.assertEquals(expected, previousWorkingDay);
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private HolidayCalendar holidayCalendar;

        @Bean
        public HolidayManager holidayManager () {
            return new HolidayManager(holidayCalendar);
        }
    }
}