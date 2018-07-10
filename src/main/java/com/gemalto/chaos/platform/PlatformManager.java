package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlatformManager {
    private static final Logger log = LoggerFactory.getLogger(PlatformManager.class);
    @Autowired(required = false)
    Collection<Platform> platforms;
    private final Map<Integer, Pair<Platform, AttackType>> attackSchedule = new HashMap<>();

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

    public Collection<Platform> getPlatforms () {
        return platforms != null ? platforms : Collections.emptySet();
    }

    public Pair<Platform, AttackType> getAttackPlatformAndType () {
        if (!attackSchedule.keySet().contains(Calendar.getInstance().get(Calendar.YEAR))) {
            attackSchedule.clear();
        }
        Integer today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        Pair<Platform, AttackType> platformAttackTypePair = attackSchedule.get(today);
        if (platformAttackTypePair == null) {
            populateAttackPlatformAndTypeSchedule();
            return getAttackPlatformAndType();
        }
        return platformAttackTypePair;
    }

    private void populateAttackPlatformAndTypeSchedule () {
        if (!attackSchedule.keySet().contains(Calendar.getInstance().get(Calendar.YEAR))) {
            attackSchedule.clear();
            attackSchedule.putIfAbsent(Calendar.getInstance().get(Calendar.YEAR), null);
        }
        Map<Platform, List<AttackType>> platformAttackTypeMap;
        platformAttackTypeMap = getPlatforms().stream()
                                              .collect(Collectors.toMap(platform -> platform, Platform::getSupportedAttackTypes));
        List<Pair<Platform, AttackType>> platformAttackTypePairList = new ArrayList<>();
        platformAttackTypeMap.forEach((platform, attackTypes) -> {
            for (AttackType attackType : attackTypes) {
                platformAttackTypePairList.add(new Pair<>(platform, attackType));
            }
        });
        Integer day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        Collections.shuffle(platformAttackTypePairList);
        for (Pair<Platform, AttackType> pair : platformAttackTypePairList) {
            log.debug("Attack on {} will be {}", day, pair);
            attackSchedule.putIfAbsent(day++, pair);
        }
    }

    void expirePlatformCachedRosters () {
        log.info("Cached containers have been manually expired");
        getPlatforms().forEach(Platform::expireCachedRoster);
    }
}
