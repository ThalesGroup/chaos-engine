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

package com.thales.chaos.refresh;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/refresh")
@Tag(name = RefreshController.REFRESH)
public class RefreshController {
    public static final String REFRESH = "Refresh";
    @Autowired
    private RefreshManager refreshManager;

    @Operation(summary = "Trigger Spring Property refresh",
               description = "Triggers the Spring Framework backend to refresh all properties loaded from external sources (i.e., vault), and reload any Refreshable Beans that depend on them, such as Platform services",
               responses = {
                       @ApiResponse(description = "Refresh has initiated and these properties have been found to refresh",
                                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
               })
    @PostMapping
    public Collection<String> doRefresh () {
        return refreshManager.doRefresh();
    }
}
