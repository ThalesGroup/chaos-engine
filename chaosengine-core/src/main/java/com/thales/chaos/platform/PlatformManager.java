package com.thales.chaos.platform;

import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

@Component
public class PlatformManager {
    private static final Logger log = LoggerFactory.getLogger(PlatformManager.class);
    @Autowired(required = false)
    private Collection<Platform> platforms;

    @Autowired
    PlatformManager () {
    }

    PlatformManager (Collection<Platform> platforms) {
        this.platforms = platforms;
    }

    PlatformHealth getOverallHealth () {
        return getPlatformHealthOfPlatforms(getPlatforms());
    }

    private PlatformHealth getPlatformHealthOfPlatforms (Collection<Platform> platforms) {
        Optional<Platform> platform = platforms.stream()
                                               .max(Comparator.comparingInt(p -> p.getPlatformHealth()
                                                                                  .getHealthLevel()));
        return platform.isPresent() ? platform.get().getPlatformHealth() : PlatformHealth.OK;
    }

    public Collection<Platform> getPlatforms () {
        return platforms != null ? platforms : Collections.emptySet();
    }

    PlatformHealth getHealthOfPlatformLevel (PlatformLevel platformLevel) {
        return getPlatformHealthOfPlatforms(getPlatformsOfLevel(platformLevel));
    }

    Collection<Platform> getPlatformsOfLevel (PlatformLevel platformLevel) {
        return getPlatforms().stream()
                             .filter(platform -> platform.getPlatformLevel() == platformLevel)
                             .collect(Collectors.toList());
    }

    Collection<PlatformLevel> getPlatformLevels () {
        return getPlatforms().stream().map(Platform::getPlatformLevel).collect(Collectors.toCollection(HashSet::new));
    }

    void expirePlatformCachedRosters () {
        log.info("Cached containers have been manually expired");
        getPlatforms().forEach(Platform::expireCachedRoster);
    }

    public Optional<Platform> getNextPlatformForExperiment (boolean force) {
        return getPlatforms().stream()
                             .sorted(comparingLong(platform -> platform.getNextChaosTime().toEpochMilli()))
                             .filter(platform1 -> force || platform1.canExperiment())
                             .filter(Platform::hasEligibleContainersForExperiments)
                             .findFirst();
    }
}
