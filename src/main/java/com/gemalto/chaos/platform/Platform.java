package com.gemalto.chaos.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.scheduler.Scheduler;
import com.gemalto.chaos.scheduler.impl.ChaosScheduler;
import com.gemalto.chaos.util.Expiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_SELF_HEALING_INTERVAL_MINUTES;

public abstract class Platform implements AttackableObject {
    private static final Duration ROSTER_CACHE_DURATION = Duration.ofHours(1);
    private static final double DEFAULT_PROBABILITY = 0.2D;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private long averageMillisPerAttack = 14400000L;
    private Scheduler scheduler;
    private List<AttackType> supportedAttackTypes;
    private Expiring<List<Container>> roster;
    private Set<Instant> attackTimes = new HashSet<>();
    @Autowired
    @Lazy
    private HolidayManager holidayManager;

    static double calculateMTBFPercentile (float millisSinceLastAttack, float averageTimeBetweenAttacks) {
        return 1 - Math.pow(0.5, millisSinceLastAttack / averageTimeBetweenAttacks);
    }

    void expireCachedRoster () {
        if (roster != null) roster.expire();
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

    public synchronized List<Container> getRoster () {
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

    @Override
    public synchronized boolean canAttack () {
        return getScheduler().getNextChaosTime().isBefore(Instant.now());
    }

    private Scheduler getScheduler () {
        if (scheduler == null) initScheduler();
        return scheduler;
    }

    private void initScheduler () {
        scheduler = ChaosScheduler.builder()
                                  .withAverageMillisBetweenExperiments(averageMillisPerAttack)
                                  .withHolidayManager(holidayManager)
                                  .build();
    }

    public Instant getNextChaosTime () {
        return getScheduler().getNextChaosTime();
    }

    public Platform startAttack () {
        getScheduler().startAttack();
        attackTimes.add(Instant.now());
        return this;
    }

    @SuppressWarnings("unused") // public get methods are consumed by Spring Rest.
    public Set<Instant> getAttackTimes () {
        return attackTimes;
    }

    public void usingHolidayManager (HolidayManager holidayManager) {
        this.holidayManager = holidayManager;
    }

    @JsonIgnore
    public Duration getMinimumSelfHealingInterval () {
        return Duration.ofMinutes(DEFAULT_SELF_HEALING_INTERVAL_MINUTES);
    }

    public List<Container> generateExperimentRoster () {
        return getRoster();
    }
}
