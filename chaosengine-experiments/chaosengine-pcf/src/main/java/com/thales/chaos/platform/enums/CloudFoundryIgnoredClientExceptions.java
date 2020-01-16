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

package com.thales.chaos.platform.enums;

import org.cloudfoundry.client.v2.ClientV2Exception;

public enum CloudFoundryIgnoredClientExceptions {
    NOT_STAGED(170002, "CF-NotStaged", "App has not finished staging");
    private Integer code;
    private String description;
    private String errorCode;

    CloudFoundryIgnoredClientExceptions (Integer code, String errorCode, String description) {
        this.code = code;
        this.description = description;
        this.errorCode = errorCode;
    }

    public static boolean isIgnorable (ClientV2Exception exception) {
        for (CloudFoundryIgnoredClientExceptions ignorableException : CloudFoundryIgnoredClientExceptions.values()) {
            if (ignorableException.code.equals(exception.getCode()) && ignorableException.errorCode.matches(exception.getErrorCode())) {
                return true;
            }
        }
        return false;
    }
}