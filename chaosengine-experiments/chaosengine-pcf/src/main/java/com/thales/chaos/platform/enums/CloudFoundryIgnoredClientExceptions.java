package com.thales.chaos.platform.enums;

import org.cloudfoundry.client.v2.ClientV2Exception;

public enum CloudFoundryIgnoredClientExceptions {
    NOT_STAGED(170002, "CF-NotStaged", "App has not finished staging");
    private Integer code;
    private String description;
    private String errorCode;

    CloudFoundryIgnoredClientExceptions (Integer code, String errorCode, String description) {
        this.code = code;
        this.description = description;
        this.errorCode = errorCode;
    }

    public static boolean isIgnorable (ClientV2Exception exception) {
        for (CloudFoundryIgnoredClientExceptions ignorableException : CloudFoundryIgnoredClientExceptions.values()) {
            if (ignorableException.code.equals(exception.getCode()) && ignorableException.errorCode.matches(exception.getErrorCode())) {
                return true;
            }
        }
        return false;
    }
}