package com.gemalto.chaos.exception.enums;

import com.gemalto.chaos.exception.ErrorCode;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public enum CloudFoundryChaosErrorCode implements ErrorCode {
    EMPTY_RESPONSE(30000),
    NO_ROUTES(30001);
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

    private final int errorCode;
    private final String shortName;
    private final String message;

    CloudFoundryChaosErrorCode (int errorCode) {
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

    @Override
    public String getShortName () {
        return shortName;
    }
}
