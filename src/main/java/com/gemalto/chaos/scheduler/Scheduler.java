package com.gemalto.chaos.scheduler;

import java.time.Instant;

public interface Scheduler {
    Instant getNextChaosTime ();
}
