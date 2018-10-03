package com.gemalto.chaos.platform;

import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/platform")
public class PlatformController {
    @Autowired
    private PlatformManager platformManager;

    @Autowired
    PlatformController () {
    }

    PlatformController (PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    @ApiOperation(value = "Get Platforms", notes = "Returns a list of all platforms registered with the Chaos Engine, including their rosters and attack history.")
    @GetMapping
    public Collection<Platform> getPlatforms () {
        return platformManager.getPlatforms();
    }

    @ApiOperation(value = "Get Platform Health", notes = "Returns the aggregate health level of each Platform Level (i.e., IaaS, PaaS, SaaS), and an overall health level.")
    @GetMapping("/health")
    public Map<PlatformLevel, PlatformHealth> getPlatformHealth () {
        EnumMap<PlatformLevel, PlatformHealth> returnValue;
        returnValue = new EnumMap<>(PlatformLevel.class);
        for (PlatformLevel platformLevel : platformManager.getPlatformLevels()) {
            returnValue.put(platformLevel, platformManager.getHealthOfPlatformLevel(platformLevel));
        }
        returnValue.put(PlatformLevel.OVERALL, platformManager.getOverallHealth());
        return returnValue;
    }

    @ApiOperation(value = "Refresh Platform Rosters", notes = "Triggers all the platforms to expire their cached roster of containers, triggering them to be recreated on next call.")
    @PostMapping("/refresh")
    public void expirePlatformRosterCache () {
        platformManager.expirePlatformCachedRosters();
    }
}
