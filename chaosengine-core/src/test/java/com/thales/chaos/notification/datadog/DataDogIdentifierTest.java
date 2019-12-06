package com.thales.chaos.notification.datadog;

import com.thales.chaos.util.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DataDogIdentifierTest {
    private final String key = "akey";
    private final String value = "avalue";
    private final String valueWithSpace = "a value";
    private final String valueQuoted = "\"a value\"";
    private DataDogIdentifier identifier;
    private DataDogIdentifier secondIdentifier;

    @Before
    public void setUp () {
        identifier = DataDogIdentifier.dataDogIdentifier().withKey(key).withValue(value);
        secondIdentifier = DataDogIdentifier.dataDogIdentifier()
                                            .withKey(key)
                                            .withValue(StringUtils.generateRandomString(10));
    }

    @Test
    public void testToString () {
        identifier.withKey(key).withValue(value);
        assertEquals("akey=avalue", identifier.toString());
        identifier.withValue(valueWithSpace);
        assertEquals("akey=\"a value\"", identifier.toString());
        identifier.withValue(valueQuoted);
        assertEquals("akey=\"a value\"", identifier.toString());
    }

    @Test
    public void testEquals () {
        assertEquals(identifier, identifier);
        assertNotEquals(identifier, secondIdentifier);
        assertNotEquals(identifier, new Object());
    }

    @Test
    public void testHashCode () {
        assertEquals(identifier.hashCode(), identifier.hashCode());
        assertNotEquals(identifier.hashCode(), secondIdentifier.hashCode());
    }

    @Test
    public void testGetters () {
        identifier.withKey(key).withValue(value);
        assertEquals(key, identifier.getKey());
        assertEquals(value, identifier.getValue());
    }
}