package com.thales.chaos.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StringUtilsTest {
    @Test
    public void addQuotesIfNecessary () {
        assertEquals("abc", StringUtils.addQuotesIfNecessary("abc"));
        assertEquals("\"ab cd\"", StringUtils.addQuotesIfNecessary("ab cd"));
        assertEquals("\"ab cd\"", StringUtils.addQuotesIfNecessary("\"ab cd\""));
    }

    @Test
    public void camelCaseConverter () {
        assertEquals("This Is How It Is", StringUtils.convertCamelCaseToSentence("thisIsHowItIs"));
        assertEquals("Many Many Test Cases", StringUtils.convertCamelCaseToSentence("manyManyTestCases"));
        assertNotEquals("Not Camel case", StringUtils.convertCamelCaseToSentence("notCamcelcase"));
    }

    @Test
    public void trimSpaces () {
        assertEquals("short", StringUtils.trimSpaces(" short "));
        assertEquals("short", StringUtils.trimSpaces("short "));
        assertEquals("short", StringUtils.trimSpaces(" short"));
        assertEquals("longer sentence", StringUtils.trimSpaces(" longer sentence "));
        assertEquals("longer sentence", StringUtils.trimSpaces(" longer sentence"));
        assertEquals("longer sentence", StringUtils.trimSpaces("longer sentence "));
    }
}