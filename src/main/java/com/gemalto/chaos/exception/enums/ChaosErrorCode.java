package com.gemalto.chaos.exception.enums;

import com.gemalto.chaos.exception.ErrorCode;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public enum ChaosErrorCode implements ErrorCode {
    GENERIC_FAILURE(10000),
    API_EXCEPTION(15000),
    ;
    private static final ResourceBundle translationBundle;

    static {
        ResourceBundle tempResourceBundle;
        try {
            tempResourceBundle = ResourceBundle.getBundle("ChaosErrorCode", Locale.getDefault());
        } catch (MissingResourceException e) {
            tempResourceBundle = ResourceBundle.getBundle("ChaosErrorCode", Locale.US);
        }
        translationBundle = tempResourceBundle;
    }

    private int errorCode;
    private String shortName;
    private String message;

    ChaosErrorCode (int errorCode) {
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
        return translationBundle;
    }
}
