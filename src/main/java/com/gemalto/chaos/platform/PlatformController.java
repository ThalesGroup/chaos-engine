package com.gemalto.chaos.platform;

import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    public Map<PlatformLevel, PlatformHealth> getPlatformHealth () {
        EnumMap<PlatformLevel, PlatformHealth> returnValue;
        returnValue = new EnumMap<>(PlatformLevel.class);
        for (PlatformLevel platformLevel : platformManager.getPlatformLevels()) {
            returnValue.put(platformLevel, platformManager.getHealthOfPlatformLevel(platformLevel));
        }
        returnValue.put(PlatformLevel.OVERALL, platformManager.getOverallHealth());
        return returnValue;
    }
}
