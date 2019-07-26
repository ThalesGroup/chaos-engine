package com.thales.chaos.container.enums;

import org.junit.Test;

import static com.thales.chaos.container.enums.CloudFoundryApplicationRouteType.*;
import static org.junit.Assert.assertEquals;

public class CloudFoundryApplicationRouteTypeTest {
    @Test
    public void testStringMapping () {
        assertEquals(HTTP, mapFromString(""));
        assertEquals(HTTP, mapFromString(null));
        assertEquals(TCP, mapFromString("tcp"));
        assertEquals(UNKNOWN, mapFromString("bogus"));
    }
}