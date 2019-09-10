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

package com.thales.chaos.exception;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public interface ErrorCode {
    default String getFormattedMessage () {
        return getErrorCode() + ": " + getLocalizedMessage();
    }

    int getErrorCode ();

    default String getLocalizedMessage () {
        final String baseMessage = getMessage();
        try {
            return getResourceBundle().getString(baseMessage);
        } catch (MissingResourceException e) {
            return baseMessage;
        }
    }

    String getMessage ();

    ResourceBundle getResourceBundle ();

    String getShortName ();

    default Supplier<ChaosException> asChaosException () {
        return () -> new ChaosException(this);
    }

    void clearCachedResourceBundle ();
}
