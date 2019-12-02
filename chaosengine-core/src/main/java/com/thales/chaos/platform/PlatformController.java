/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.platform;

import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = PlatformController.PLATFORM)
public class PlatformController {
    public static final String PLATFORM = "Platform";
    @Autowired
    private PlatformManager platformManager;

    @Operation(summary = "Get Platforms",
               description = "Returns a list of all platforms registered with the Chaos Engine, including their rosters and experiment history.",
               responses = {
                       @ApiResponse(description = "All platforms registered in Chaos Engine",
                                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Platform.class))))
               })
    @GetMapping
    public Collection<Platform> getPlatforms () {
        return platformManager.getPlatforms();
    }

    @Operation(summary = "Get Platform Health",
               description = "Returns the aggregate health level of each Platform Level (i.e., IaaS, PaaS, SaaS), and an overall health level.",
               responses = {
                       @ApiResponse(description = "The aggregated health of all platform levels Chaos Engine is connected into.")
               })
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

    @Operation(summary = "Refresh Platform Rosters",
               description = "Triggers all the platforms to expire their cached roster of containers, triggering them to be recreated on next call.",
               responses = {
                       @ApiResponse(description = "Platform rosters have been expired and will redo lookup when next requested.")
               })
    @PostMapping("/refresh")
    public void expirePlatformRosterCache () {
        platformManager.expirePlatformCachedRosters();
    }
}
