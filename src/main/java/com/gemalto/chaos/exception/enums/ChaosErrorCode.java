package com.gemalto.chaos.exception.enums;

import com.gemalto.chaos.exception.ErrorCode;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public enum ChaosErrorCode implements ErrorCode {
    GENERIC_FAILURE(10000),
    API_EXCEPTION(11000),
    PLATFORM_DOES_NOT_SUPPORT_RECYCLING(11001);
    private final int errorCode;
    private final String shortName;
    private final String message;
    private ResourceBundle translationBundle;

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
