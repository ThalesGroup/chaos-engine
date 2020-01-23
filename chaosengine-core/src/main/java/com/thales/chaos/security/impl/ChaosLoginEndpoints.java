/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

package com.thales.chaos.security.impl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.thales.chaos.security.impl.ChaosLoginEndpoints.SECURITY;

@RestController
@Tag(name = SECURITY)
public class ChaosLoginEndpoints {
    public static final String SECURITY = "Security";

    @Operation(summary = "Login", responses = {
            @ApiResponse(description = "Successful authentication",
                         headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string", example = "JSESSIONID=abcde12345; Path=/; HttpOnly"))),
            @ApiResponse(responseCode = "404", description = "Login unsuccessful")
    })
    @PostMapping("/login")
    public void login (@Parameter(name = "username") String username, @Parameter(name = "password") String password) {
        throw new IllegalStateException("This method should not be reachable. It exists for SpringDocs Autogeneration");
    }

    @Operation(summary = "Logout", responses = {
            @ApiResponse(description = "Successful logout",
                         headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string", example = "JSESSIONID=abcde12345; Path=/; HttpOnly; Max-Age=0"))),
            @ApiResponse(responseCode = "404", description = "Logout unsuccessful")
    })
    @PostMapping("/logout")
    public void logout (@Parameter(name = "JSESSIONID", in = ParameterIn.COOKIE) String jsessionid) {
        throw new IllegalStateException("This method should not be reachable. It exists for SpringDocs Autogeneration");
    }
}
