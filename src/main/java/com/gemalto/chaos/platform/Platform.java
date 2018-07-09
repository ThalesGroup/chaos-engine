package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public abstract class Platform implements AttackableObject {
    private static final double DEFAULT_PROBABILITY = 0.2D;
    Integer averageAttackFrequencyDays = 7;
    Duration averageAttackDuration = Duration.ofHours(12);
    Instant previousAttackTime = Instant.now();
    private boolean canAttackNow = false;

    public abstract List<Container> getRoster ();

    public abstract ApiStatus getApiStatus ();

    public abstract PlatformLevel getPlatformLevel ();

    public abstract PlatformHealth getPlatformHealth ();

    @Override
    public boolean canAttack () {
        return true;
    }

    public double getDestructionProbability () {
        return DEFAULT_PROBABILITY;
    }

    public String getPlatformType () {
        return this.getClass().getSimpleName();
    }
}
