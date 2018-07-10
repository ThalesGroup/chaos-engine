package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.AttackableObject;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Platform implements AttackableObject {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final double DEFAULT_PROBABILITY = 0.2D;
    private List<AttackType> supportedAttackTypes;

    public abstract List<Container> getRoster ();

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
