package com.thales.chaos.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.experiment.ExperimentalObject;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.scheduler.Scheduler;
import com.thales.chaos.scheduler.impl.ChaosScheduler;
import com.thales.chaos.util.Expiring;
import com.thales.chaos.constants.DataDogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thales.chaos.constants.ExperimentConstants.DEFAULT_SELF_HEALING_INTERVAL_MINUTES;
import static java.util.stream.Collectors.toSet;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

public abstract class Platform implements ExperimentalObject {
    private static final Duration ROSTER_CACHE_DURATION = Duration.ofHours(1);
    private static final double DEFAULT_PROBABILITY = 0.2D;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private long averageMillisPerExperiment = 14400000L;
    private Scheduler scheduler;
    private List<ExperimentType> supportedExperimentTypes;
    private Expiring<List<Container>> roster;
    private Set<Instant> experimentTimes = new HashSet<>();
    @Autowired
    @Lazy
    private HolidayManager holidayManager;

    public void setAverageMillisPerExperiment (long averageMillisPerExperiment) {

        if (averageMillisPerExperiment == this.averageMillisPerExperiment) return;
        log.info("Setting average time between failure for {} to {} ms", this, averageMillisPerExperiment);
        this.averageMillisPerExperiment = averageMillisPerExperiment;
        this.scheduler = null;
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

    List<ExperimentType> getSupportedExperimentTypes () {
        if (supportedExperimentTypes == null) {
            supportedExperimentTypes = getRoster().stream()
                                                  .map(Container::getSupportedExperimentTypes)
                                                  .flatMap(List::stream)
                                                  .distinct()
                                                  .collect(Collectors.toList());
        }
        return supportedExperimentTypes;
    }

    public synchronized List<Container> getRoster () {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(DataDogConstants.DATADOG_PLATFORM_KEY, this.getPlatformType())) {
            if (roster == null) {
                roster = new Expiring<>(generateRoster(), ROSTER_CACHE_DURATION);
            }
            return roster.computeIfAbsent(this::generateRoster, ROSTER_CACHE_DURATION);
        }
    }

    protected abstract List<Container> generateRoster ();

    @Override
    public synchronized boolean canExperiment () {
        return getScheduler().getNextChaosTime().isBefore(Instant.now());
    }

    private Scheduler getScheduler () {
        if (scheduler == null) initScheduler();
        return scheduler;
    }

    private void initScheduler () {
        scheduler = ChaosScheduler.builder().withAverageMillisBetweenExperiments(averageMillisPerExperiment)
                                  .withHolidayManager(holidayManager)
                                  .build();
    }

    public Instant getNextChaosTime () {
        return getScheduler().getNextChaosTime();
    }

    public Platform scheduleExperiment () {
        expireCachedRoster();
        log.info("Scheduling an experiment on {}", keyValue(DataDogConstants.DATADOG_PLATFORM_KEY, this.getPlatformType()));
        getScheduler().startExperiment();
        experimentTimes.add(Instant.now());
        return this;
    }

    @SuppressWarnings("unused") // public get methods are consumed by Spring Rest.
    public Set<Date> getExperimentTimes () {
        // Instant to String conversion is done due to Swagger which does not support java.time classes - SCT-6607
        return experimentTimes.stream().map(Date::from).collect(toSet());
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

    public abstract boolean isContainerRecycled (Container container);
}
