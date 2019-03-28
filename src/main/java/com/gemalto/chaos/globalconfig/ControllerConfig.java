package com.gemalto.chaos.globalconfig;

import com.gemalto.chaos.ChaosEngine;
import com.gemalto.chaos.exception.ChaosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice
public class ControllerConfig {
    @ExceptionHandler(ChaosException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleChaosException (ChaosException ex) {
        final Logger logger = Arrays.stream(ex.getStackTrace())
                                    .map(StackTraceElement::getClassName)
                                    .filter(s -> s.startsWith("com.gemalto.chaos"))
                                    .findFirst()
                                    .map(LoggerFactory::getLogger)
                                    .orElseGet(() -> LoggerFactory.getLogger(ChaosEngine.class));
        logger.error("Unhandled ChaosException in HTTP Request", ex);
    }
}
