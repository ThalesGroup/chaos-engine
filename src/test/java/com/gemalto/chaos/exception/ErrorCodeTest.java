package com.gemalto.chaos.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class ErrorCodeTest {
    private static <T extends ErrorCode> Collection<ErrorCode> getAllErrorCodes (Class<T> clazz) {
        return Arrays.asList(clazz.getEnumConstants());
    }

    public static class USLocaleOnly {
        private Collection<Collection<ErrorCode>> allErrorCodes = new HashSet<>();

        @Before
        public void setUp () {
            Locale.setDefault(Locale.US);
            allErrorCodes = new Reflections("com.gemalto.chaos").getSubTypesOf(ErrorCode.class)
                                                                .stream()
                                                                .map(ErrorCodeTest::getAllErrorCodes)
                                                                .peek(errorCodes -> errorCodes.forEach(ErrorCode::clearCachedResourceBundle))
                                                                .collect(Collectors.toList());
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
        public void getFormattedMessage () {
            allErrorCodes.stream()
                         .flatMap(Collection::stream)
                         .peek(errorCode -> assertThat(errorCode.getFormattedMessage(), startsWith(String.valueOf(errorCode
                                 .getErrorCode()))))
                         .forEach(errorCode -> assertThat(errorCode.getFormattedMessage(), endsWith(errorCode.getLocalizedMessage())));
        }
    }

    @RunWith(Parameterized.class)
    public static class LocaleTests {
        private final Locale locale;
        private Collection<Collection<ErrorCode>> allErrorCodes = new HashSet<>();

        public LocaleTests (Locale locale) {
            this.locale = locale;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> getLocales () {
            final List<Object[]> locales = new ArrayList<>();
            locales.add(new Locale[]{ Locale.US });
            for (Locale availableLocale : Locale.getAvailableLocales()) {
                locales.add(new Locale[]{ availableLocale });
            }
            return locales;
        }

        @Before
        public void setUp () {
            Locale.setDefault(locale);
            allErrorCodes = new Reflections("com.gemalto.chaos").getSubTypesOf(ErrorCode.class)
                                                                .stream()
                                                                .map(ErrorCodeTest::getAllErrorCodes)
                                                                .peek(errorCodes -> errorCodes.forEach(ErrorCode::clearCachedResourceBundle))
                                                                .collect(Collectors.toList());
        }

        @Test
        public void containsTranslation () {
            allErrorCodes.stream().flatMap(Collection::stream).forEach(errorCode -> {
                final String message = errorCode.getMessage();
                final String localizedMessage = errorCode.getLocalizedMessage();
                assertNotEquals(message + " should contain a translation for locale " + locale, message, localizedMessage);
            });
        }
    }
}