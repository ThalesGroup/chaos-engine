package com.gemalto.chaos.exception;

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
