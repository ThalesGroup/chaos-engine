package com.gemalto.chaos.scheduler.impl;

import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.scheduler.Scheduler;

import java.time.Instant;
import java.util.Random;

public class ChaosScheduler implements Scheduler {
    private long averageMillisBetweenExperiments;
    private Instant nextChaosTime;
    private Instant lastChaosTime;
    private HolidayManager holidayManager;


    @Override
    public Instant getNextChaosTime () {
        if (nextChaosTime != null) return nextChaosTime;
        if (lastChaosTime != null) return calculateNextChaosTime();
        return calculateFirstChaosTime();
    }

    private Instant calculateNextChaosTime () {
        nextChaosTime = getTimeAfterWorkingMillis(lastChaosTime, (long) (getRandomScalingFactor() * averageMillisBetweenExperiments));
        return nextChaosTime;
    }

    private Instant calculateFirstChaosTime () {
        nextChaosTime = getTimeAfterWorkingMillis((long) (Math.sqrt(getRandomScalingFactor()) * averageMillisBetweenExperiments));
        return nextChaosTime;
    }

    private Instant getTimeAfterWorkingMillis (Instant start, long workingMillis) {
        // TODO Implement
        return null;
    }

    private static double getRandomScalingFactor () {
        double gaussian;
        do {
            gaussian = new Random().nextGaussian() + 0.5;
        } while (gaussian <= 0 || gaussian > 1);
        return Math.log(1 - gaussian) / Math.log(0.5);
    }

    private Instant getTimeAfterWorkingMillis (long workingMillis) {
        return getTimeAfterWorkingMillis(Instant.now(), workingMillis); // TODO Implement
    }

    public void startAttack () {
        lastChaosTime = nextChaosTime;
        nextChaosTime = null;
    }

    public static ChaosSchedulerBuilder builder () {
        return ChaosSchedulerBuilder.aChaosScheduler();
    }

    public static final class ChaosSchedulerBuilder {
        private long averageMillisBetweenExperiments;
        private Instant lastChaosTime;
        private HolidayManager holidayManager;

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

        public ChaosScheduler build () {
            ChaosScheduler chaosScheduler = new ChaosScheduler();
            chaosScheduler.lastChaosTime = this.lastChaosTime;
            chaosScheduler.averageMillisBetweenExperiments = this.averageMillisBetweenExperiments;
            chaosScheduler.holidayManager = this.holidayManager;
            return chaosScheduler;
        }
    }
}
