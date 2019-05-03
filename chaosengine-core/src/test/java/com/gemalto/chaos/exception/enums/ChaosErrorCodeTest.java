package com.gemalto.chaos.exception.enums;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class ChaosErrorCodeTest {
    private Collection<ChaosErrorCode> allChaosErrorCodes;

    @Before
    public void setUp () {
        allChaosErrorCodes = Arrays.asList(ChaosErrorCode.values());
    }

    @Test
    public void getMessage () {
        allChaosErrorCodes.stream()
                          .peek(errorCode -> assertThat(errorCode.getMessage(), containsString(Integer.toString(errorCode
                                  .getErrorCode()))))
                          .peek(errorCode -> assertThat(errorCode.getMessage(), CoreMatchers.endsWith(".message")))
                          .forEach(errorCode -> assertThat(errorCode.getMessage(), CoreMatchers.startsWith("errorCode.")));
    }
}