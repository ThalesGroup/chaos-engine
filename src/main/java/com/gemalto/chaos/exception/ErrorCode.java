package com.gemalto.chaos.exception;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
}
