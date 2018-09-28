package com.gemalto.chaos.scheduler.impl;

import com.gemalto.chaos.calendar.HolidayManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChaosSchedulerTest {
    @Mock
    private static Random random;
    private static long n = 14400000;
    @MockBean
    private HolidayManager holidayManager;
    private Instant now = Instant.now();
    @Autowired
    private ChaosScheduler scheduler;

    @Test
    public void getNextChaosTime () {
        final long Millis1 = 11424173L;
        final double Gauss1 = 0D;
        doReturn(Gauss1).when(random).nextGaussian();
        doReturn(now.plusMillis(Millis1)).when(holidayManager)
                                         .getInstantAfterWorkingMillis(any(Instant.class), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime());
        verify(holidayManager, times(1)).getInstantAfterWorkingMillis(any(Instant.class), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime()); // Two asserts to test caching.
    }

    @Test
    public void getNextChaosTimeWithLastChaosTime () {
        scheduler = ChaosScheduler.builder()
                                  .withAverageMillisBetweenExperiments(n)
                                  .withHolidayManager(holidayManager)
                                  .withRandom(random)
                                  .withLastChaosTime(now)
                                  .build();
        final long Millis1 = 9063314L;
        final double Gauss1 = 0D;
        doReturn(Gauss1).when(random).nextGaussian();
        doReturn(now.plusMillis(Millis1)).when(holidayManager).getInstantAfterWorkingMillis(eq(now), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime());
        verify(holidayManager, times(1)).getInstantAfterWorkingMillis(eq(now), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime()); // Two asserts to test caching.
    }

    @Configuration
    static class ChaosSchedulerTestConfig {
        @Autowired
        private HolidayManager holidayManager;

        @Bean
        ChaosScheduler chaosScheduler () {
            return ChaosScheduler.builder()
                                 .withAverageMillisBetweenExperiments(n)
                                 .withHolidayManager(holidayManager)
                                 .withRandom(random)
                                 .build();
        }
    }
}