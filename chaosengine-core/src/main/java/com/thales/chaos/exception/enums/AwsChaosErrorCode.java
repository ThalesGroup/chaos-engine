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

package com.thales.chaos.exception.enums;

import com.thales.chaos.exception.ErrorCode;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public enum AwsChaosErrorCode implements ErrorCode {
    AWS_PERMISSION_ERROR(21001),
    AWS_EC2_GENERIC_API_ERROR(21100),
    NO_INSTANCES_RETURNED(21101),
    AWS_RDS_GENERIC_API_ERROR(21200),
    RDS_DOES_NOT_SUPPORT_RECYCLING(21201),
    RDS_NO_VPC_FOUND(21202),
    INVALID_SNAPSHOT_NAME(21203),
    NOT_PART_OF_ASG(22101),
    SINGLE_INSTANCE_CLUSTER(22201),
    INVALID_CIDR_BLOCK(21103),
    RDS_SNAPSHOT_EXISTS(21204),
    RDS_IN_INVALID_STATE(21205),
    RDS_INSTANCE_NOT_FOUND(21206),
    RDS_SNAPSHOT_QUOTA_EXCEEDED(21207),
    RDS_INVALID_SNAPSHOT(21208),
    RDS_RUNTIME_ERROR(21209),
    ;
    private final int errorCode;
    private final String shortName;
    private final String message;
    private ResourceBundle translationBundle;

    AwsChaosErrorCode (int errorCode) {
        this.errorCode = errorCode;
        this.shortName = "errorCode." + errorCode + ".name";
        this.message = "errorCode." + errorCode + ".message";
    }

    @Override
    public int getErrorCode () {
        return errorCode;
    }

    @Override
    public String getMessage () {
        return message;
    }

    @Override
    public ResourceBundle getResourceBundle () {
        return Optional.ofNullable(translationBundle).orElseGet(this::initTranslationBundle);
    }

    private synchronized ResourceBundle initTranslationBundle () {
        if (translationBundle != null) return translationBundle;
        try {
            final Locale defaultLocale = Locale.getDefault();
            translationBundle = ResourceBundle.getBundle("exception.ChaosErrorCode", defaultLocale);
        } catch (MissingResourceException e) {
            translationBundle = ResourceBundle.getBundle("exception.ChaosErrorCode", Locale.US);
        }
        return translationBundle;
    }

    @Override
    public String getShortName () {
        return shortName;
    }

    @Override
    public void clearCachedResourceBundle () {
        translationBundle = null;
    }
}
