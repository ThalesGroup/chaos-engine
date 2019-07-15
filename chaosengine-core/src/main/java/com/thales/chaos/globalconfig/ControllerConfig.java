package com.thales.chaos.globalconfig;

import com.thales.chaos.util.ErrorUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerConfig {
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleChaosException (Exception ex) {
        ErrorUtils.logStacktraceUsingLastValidLogger(ex, "Unhandled Exception in HTTP Request");
        return ex.getMessage();
    }
}
