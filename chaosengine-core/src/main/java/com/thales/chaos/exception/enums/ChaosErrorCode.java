package com.thales.chaos.exception.enums;

import com.thales.chaos.exception.ErrorCode;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public enum ChaosErrorCode implements ErrorCode {
    GENERIC_FAILURE(10000),
    API_EXCEPTION(11000),
    EXPERIMENT_START_FAILURE(12001),
    RECYCLING_UNSUPPORTED(12002),
    PLATFORM_DOES_NOT_SUPPORT_SHELL(12003),
    SELF_HEALING_CALL_ERROR(13001),
    ERROR_CREATING_EXPERIMENT_METHOD_FROM_JAVA(13002),
    PLATFORM_DOES_NOT_SUPPORT_SHELL_EXPERIMENTS(13003),
    INVALID_STATE(19001),
    NOTIFICATION_SEND_ERROR(18001),
    NOTIFICATION_BUFFER_ERROR(18201),
    NOTIFICATION_BUFFER_RETRY_EXCEEDED(18202),
    PLATFORM_DOES_NOT_SUPPORT_RECYCLING(11001),
    SHELL_CLIENT_CONNECT_FAILURE(15001),
    SHELL_CLIENT_TRANSFER_ERROR(15002),
    SHELL_CLIENT_COMMAND_ERROR(15003),
    SHELL_CLIENT_DEPENDENCY_ERROR(15004),
    SSH_CLIENT_INSTANTIATION_ERROR(15101),
    SSH_CLIENT_TRANSFER_ERROR(15102),
    SSH_CLIENT_COMMAND_ERROR(15103),
    SSH_CREDENTIAL_PASSWORD_CALL_FAILURE(15201),
    SSH_CREDENTIALS_INVALID_KEY_FORMAT(15202),
    SHELL_SCRIPT_READ_FAILURE(15901),
    SHELL_SCRIPT_LOOKUP_FAILURE(15902),
    SHELL_SCRIPT_FORMATTING_ERROR(15903);
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
