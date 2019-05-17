package com.thales.chaos.globalconfig;

import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.util.ErrorUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerConfig {
    @ExceptionHandler(ChaosException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleChaosException (ChaosException ex) {
        ErrorUtils.logStacktraceUsingLastValidLogger(ex, "Unhandled ChaosException in HTTP Request");
    }
}
