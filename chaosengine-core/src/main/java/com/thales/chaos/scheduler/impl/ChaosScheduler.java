/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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
import com.thales.chaos.constants.MathUtils;
import com.thales.chaos.scheduler.Scheduler;

import java.time.Instant;
import java.util.Random;

public class ChaosScheduler implements Scheduler {
    private long averageMillisBetweenExperiments;
    private Instant nextChaosTime;
    private Instant lastChaosTime;
    private HolidayManager holidayManager;
    private Random random;

    public static ChaosSchedulerBuilder builder () {
        return ChaosSchedulerBuilder.aChaosScheduler();
    }

    @Override
    public Instant getNextChaosTime () {
        if (nextChaosTime != null) return nextChaosTime;
        if (lastChaosTime != null) return calculateNextChaosTime();
        return calculateFirstChaosTime();
    }

    private Instant calculateNextChaosTime () {
        nextChaosTime = getInstantAfterWorkingMillis(lastChaosTime, (long) (getRandomScalingFactor() * averageMillisBetweenExperiments));
        return nextChaosTime;
    }

    private Instant calculateFirstChaosTime () {
        nextChaosTime = getInstantAfterWorkingMillis((long) (Math.sqrt(getRandomScalingFactor()) * averageMillisBetweenExperiments));
        return nextChaosTime;
    }

    private Instant getInstantAfterWorkingMillis (Instant start, long workingMillis) {
        return holidayManager.getInstantAfterWorkingMillis(start, workingMillis);
    }

    private double getRandomScalingFactor () {
        double gaussian;
        do {
            gaussian = (random.nextGaussian() + 1) * 0.5 / MathUtils.RAMANUJAN_SOLDNER_CONSTANT;
        } while (gaussian <= 0.01 || gaussian >= 0.99999);
        return Math.log(1 - gaussian) / Math.log(0.5);
    }

    private Instant getInstantAfterWorkingMillis (long workingMillis) {
        return getInstantAfterWorkingMillis(Instant.now(), workingMillis);
    }

    @Override
    public void startExperiment () {
        lastChaosTime = nextChaosTime;
        nextChaosTime = null;
    }

    public static final class ChaosSchedulerBuilder {
        private long averageMillisBetweenExperiments;
        private Instant lastChaosTime;
        private HolidayManager holidayManager;
        private Random random = new Random();

        private ChaosSchedulerBuilder () {
        }

        private static ChaosSchedulerBuilder aChaosScheduler () {
            return new ChaosSchedulerBuilder();
        }

        public ChaosSchedulerBuilder withAverageMillisBetweenExperiments (long averageMillisBetweenExperiments) {
            this.averageMillisBetweenExperiments = averageMillisBetweenExperiments;
            return this;
        }

        public ChaosSchedulerBuilder withLastChaosTime (Instant lastChaosTime) {
            this.lastChaosTime = lastChaosTime;
            return this;
        }

        public ChaosSchedulerBuilder withHolidayManager (HolidayManager holidayManager) {
            this.holidayManager = holidayManager;
            return this;
        }

        public ChaosSchedulerBuilder withRandom (Random random) {
            this.random = random;
            return this;
        }

        public ChaosScheduler build () {
            ChaosScheduler chaosScheduler = new ChaosScheduler();
            chaosScheduler.lastChaosTime = this.lastChaosTime;
            chaosScheduler.averageMillisBetweenExperiments = this.averageMillisBetweenExperiments;
            chaosScheduler.holidayManager = this.holidayManager;
            chaosScheduler.random = this.random;
            return chaosScheduler;
        }
    }
}
