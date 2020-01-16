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

package com.thales.chaos.notification.datadog;

import com.thales.chaos.constants.DataDogConstants;

import static com.thales.chaos.util.StringUtils.addQuotesIfNecessary;

public class DataDogIdentifier {
    private String key = DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY;
    private String value;

    private DataDogIdentifier () {
    }

    public static DataDogIdentifier dataDogIdentifier () {
        return new DataDogIdentifier();
    }

    @Override
    public int hashCode () {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataDogIdentifier that = (DataDogIdentifier) o;
        if (!key.equals(that.key)) return false;
        return value.equals(that.value);
    }

    public String getKey () {
        return key;
    }

    public String getValue () {
        return value;
    }

    public DataDogIdentifier withKey (String key) {
        this.key = key;
        return this;
    }

    public DataDogIdentifier withValue (String value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString () {
        return addQuotesIfNecessary(key) + "=" + addQuotesIfNecessary(value);
    }
}
