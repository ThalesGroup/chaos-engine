/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.scheduler.impl;

import com.thales.chaos.calendar.HolidayManager;
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
        final long Millis1 = 11240648L;
        final double Gauss1 = 0D;
        doReturn(Gauss1).when(random).nextGaussian();
        doReturn(now.plusMillis(Millis1)).when(holidayManager).getInstantAfterWorkingMillis(any(Instant.class), eq(Millis1));
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
        final long Millis1 = 8774457L;
        final double Gauss1 = 0D;
        doReturn(Gauss1).when(random).nextGaussian();
        doReturn(now.plusMillis(Millis1)).when(holidayManager).getInstantAfterWorkingMillis(eq(now), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime());
        verify(holidayManager, times(1)).getInstantAfterWorkingMillis(eq(now), eq(Millis1));
        assertEquals(now.plusMillis(Millis1), scheduler.getNextChaosTime()); // Two asserts to test caching.
    }

    @Test
    public void testScalingFactorBoundaries () {
        doReturn(-1D, 2 * Math.sqrt(2) - 0.9, 0D).when(random).nextGaussian();
        scheduler.getNextChaosTime();
        verify(random, times(3)).nextGaussian();
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