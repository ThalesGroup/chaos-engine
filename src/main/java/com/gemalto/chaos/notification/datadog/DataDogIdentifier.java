package com.gemalto.chaos.notification.datadog;

import com.gemalto.chaos.constants.DataDogConstants;

public class DataDogIdentifier {
    private String key = DataDogConstants.DEFAULT_DATADOG_IDENTIFIER_KEY;
    private String value;

    private DataDogIdentifier () {
    }

    public static DataDogIdentifier dataDogIdentifier () {
        return new DataDogIdentifier();
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
}
