package com.gemalto.chaos.notification.datadog;

import com.gemalto.chaos.constants.DataDogConstants;

import static com.gemalto.chaos.util.StringUtils.addQuotesIfNecessary;

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
