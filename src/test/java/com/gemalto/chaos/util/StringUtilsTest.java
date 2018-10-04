package com.gemalto.chaos.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {
    @Test
    public void addQuotesIfNecessary () {
        assertEquals("abc", StringUtils.addQuotesIfNecessary("abc"));
        assertEquals("\"ab cd\"", StringUtils.addQuotesIfNecessary("ab cd"));
        assertEquals("\"ab cd\"", StringUtils.addQuotesIfNecessary("\"ab cd\""));
    }
}