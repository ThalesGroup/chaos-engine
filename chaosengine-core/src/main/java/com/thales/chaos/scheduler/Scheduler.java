package com.thales.chaos.scheduler;

import java.time.Instant;

public interface Scheduler {
    Instant getNextChaosTime ();

    void startExperiment ();
}
