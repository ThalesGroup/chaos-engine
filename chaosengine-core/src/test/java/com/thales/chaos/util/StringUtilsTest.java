/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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