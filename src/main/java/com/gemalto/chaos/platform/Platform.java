package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.util.Expiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Platform implements AttackableObject {
    private static final Duration ROSTER_CACHE_DURATION = Duration.ofHours(1);
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final double DEFAULT_PROBABILITY = 0.2D;
    private List<AttackType> supportedAttackTypes;
    private Expiring<List<Container>> roster;

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
}
