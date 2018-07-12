package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.util.Expiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Platform implements AttackableObject {
    private static final Duration ROSTER_CACHE_DURATION = Duration.ofHours(1);
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final double DEFAULT_PROBABILITY = 0.2D;
    private List<AttackType> supportedAttackTypes;
    private Expiring<List<Container>> roster;
    private static final int MIN_ATTACKS_PER_PERIOD = 3;
    private static final int MAX_ATTACKS_PER_PERIOD = 5;
    private TemporalUnit attackPeriod = ChronoUnit.DAYS;
    private Set<Instant> attackTimes = new HashSet<>();
    private Instant lastAttackCheckTime = Instant.now();
    private Instant lastAttackTime = Instant.now();
    private HolidayManager holidayManager;

    public List<Container> getRoster () {
        List<Container> returnValue;
        if (roster == null || roster.value() == null) {
            returnValue = generateRoster();
            roster = new Expiring<>(returnValue, ROSTER_CACHE_DURATION);
            return returnValue;
        }
        returnValue = roster.value();
        if (returnValue == null) {
            returnValue = generateRoster();
            roster = new Expiring<>(returnValue, ROSTER_CACHE_DURATION);
        }
        return returnValue;
    }

    protected abstract List<Container> generateRoster ();

    void expireCachedRoster () {
        roster.expire();
    }

    public abstract ApiStatus getApiStatus ();

    public abstract PlatformLevel getPlatformLevel ();

    public abstract PlatformHealth getPlatformHealth ();

    public double getDestructionProbability () {
        return DEFAULT_PROBABILITY;
    }

    public String getPlatformType () {
        return this.getClass().getSimpleName();
    }

    List<AttackType> getSupportedAttackTypes () {
        if (supportedAttackTypes == null) {
            supportedAttackTypes = getRoster().stream()
                                              .map(Container::getSupportedAttackTypes)
                                              .flatMap(List::stream)
                                              .distinct()
                                              .collect(Collectors.toList());
        }
        return supportedAttackTypes;
    }

    @Override
    public boolean canAttack () {
        if (belowMinAttacks()) {
            log.info("{} is below its attack quota. Attacking.", this.getClass().getSimpleName());
            return true;
        }
        if (aboveMaxAttacks()) {
            log.info("{} is above its attack quota. Aborting.", this.getClass().getSimpleName());
            return false;
        }
        return new Random().nextDouble() < getAttackChance();
        // TODO : This takes a LONG time to diverge. It needs to be improved upon
    }

    private boolean belowMinAttacks () {
        return getAttacksInPeriod() < MIN_ATTACKS_PER_PERIOD;
    }

    private boolean aboveMaxAttacks () {
        return getAttacksInPeriod() >= MAX_ATTACKS_PER_PERIOD;
    }

    private double getAttackChance () {
        double fudgeFactor;
        long millisSinceLastAttack;
        long averageTimeBetweenAttacks;
        long millisSinceLastCheck;
        if (attackPeriod.getDuration().toMillis() > ChronoUnit.DAYS.getDuration().toMillis()) {
            // Handle if probability is in weeks/months
            averageTimeBetweenAttacks = holidayManager.getWorkingMillisInDuration(attackPeriod.getDuration()) / ((MAX_ATTACKS_PER_PERIOD + MIN_ATTACKS_PER_PERIOD) / 2);
            millisSinceLastCheck = holidayManager.getWorkingMillisSinceInstant(lastAttackCheckTime);
            millisSinceLastAttack = holidayManager.getWorkingMillisSinceInstant(lastAttackTime);
        } else {
            averageTimeBetweenAttacks = holidayManager.getTotalMillisInDay() / ((MAX_ATTACKS_PER_PERIOD + MIN_ATTACKS_PER_PERIOD) / 2);
            long now = Instant.now().toEpochMilli();
            Instant startOfDay = holidayManager.getStartOfDay();
            millisSinceLastCheck = now - lastAttackCheckTime.toEpochMilli() - (lastAttackCheckTime.isBefore(startOfDay) ? holidayManager
                    .getOvernightMillis() : 0);
            long lastAttackTimeMillis = lastAttackTime.toEpochMilli();
            millisSinceLastAttack = now - lastAttackTimeMillis - (lastAttackTime.isBefore(startOfDay) ? holidayManager.getOvernightMillis() : 0);
            if (millisSinceLastAttack == 0 || millisSinceLastCheck == 0) return 0;
        }
        fudgeFactor = Math.pow(Math.E, -1 * millisSinceLastAttack / (double) averageTimeBetweenAttacks) * -1 + 1;
        fudgeFactor *= millisSinceLastCheck / (double) averageTimeBetweenAttacks;
        log.debug("Chance of attack: {}", fudgeFactor);
        log.debug("Based on: millisSinceLastAttack: {} averageTimeBetweenAttacks: {}", millisSinceLastAttack, averageTimeBetweenAttacks);
        updateLastAttackCheckTime();
        return fudgeFactor;
    }

    private void updateLastAttackCheckTime () {
        lastAttackCheckTime = Instant.now();
    }

    private int getAttacksInPeriod () {
        Instant beginningOfAttackPeriod;
        if (holidayManager != null && attackPeriod == ChronoUnit.DAYS) {
            // If we're dealing in days, we need prune this down to working days.
            beginningOfAttackPeriod = holidayManager.getPreviousWorkingDay();
        } else {
            beginningOfAttackPeriod = Instant.now().minus(1, attackPeriod);
        }
        return (int) attackTimes.stream().filter(instant -> instant.isAfter(beginningOfAttackPeriod)).count();
    }

    public Platform startAttack () {
        lastAttackTime = Instant.now();
        attackTimes.add(lastAttackTime);
        return this;
    }

    @SuppressWarnings("unused") // public get methods are consumed by Spring Rest.
    public Set<Instant> getAttackTimes () {
        return attackTimes;
    }

    public Platform usingHolidayManager (HolidayManager holidayManager) {
        this.holidayManager = holidayManager;
        return this;
    }
}
