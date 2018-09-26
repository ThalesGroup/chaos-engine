package com.gemalto.chaos.scheduler.impl;

import com.gemalto.chaos.scheduler.Scheduler;

import java.time.Instant;
import java.util.Random;

public class ChaosScheduler implements Scheduler {
    private long averageMillisBetweenExperiments;
    private Instant nextChaosTime;
    private Instant lastChaosTime;

    public ChaosScheduler (long averageMillisBetweenExperiments) {
        this.averageMillisBetweenExperiments = averageMillisBetweenExperiments;
    }

    public ChaosScheduler (long averageMillisBetweenExperiments, Instant lastChaosTime) {
        this.averageMillisBetweenExperiments = averageMillisBetweenExperiments;
        this.lastChaosTime = lastChaosTime;
    }

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
}
