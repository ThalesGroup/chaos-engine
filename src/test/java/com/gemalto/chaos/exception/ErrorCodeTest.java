package com.gemalto.chaos.exception;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ErrorCodeTest {
    private Collection<Collection<ErrorCode>> allErrorCodes = new HashSet<>();

    @Before
    public void setUp () {
        Locale.setDefault(Locale.US);
        allErrorCodes = new Reflections("com.gemalto.chaos").getSubTypesOf(ErrorCode.class)
                                                            .stream()
                                                            .map(this::getAllErrorCodes)
                                                            .collect(Collectors.toList());
    }

    private <T extends ErrorCode> Collection<ErrorCode> getAllErrorCodes (Class<T> clazz) {
        return Arrays.asList(clazz.getEnumConstants());
    }

    @Test
    public void noDuplicateErrorCodes () {
        Set<Integer> usedErrorCodes = new HashSet<>();
        allErrorCodes.stream()
                     .flatMap(Collection::stream)
                     .map(ErrorCode::getErrorCode)
                     .forEach(integer -> assertTrue("Reused error code " + integer, usedErrorCodes.add(integer)));
    }

    @Test
    public void containsTranslation () {
        allErrorCodes.stream().flatMap(Collection::stream).forEach(errorCode -> {
            final String message = errorCode.getMessage();
            final String localizedMessage = errorCode.getLocalizedMessage();
            assertNotEquals(message + " should contain a translation", message, localizedMessage);
        });
    }

    @Test
    public void getFormattedMessage () {
        allErrorCodes.stream()
                     .flatMap(Collection::stream)
                     .peek(errorCode -> assertThat(errorCode.getFormattedMessage(), CoreMatchers.startsWith(String.valueOf(errorCode
                             .getErrorCode()))))
                     .forEach(errorCode -> assertThat(errorCode.getFormattedMessage(), CoreMatchers.endsWith(errorCode.getLocalizedMessage())));
    }
}