package com.gemalto.chaos.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ExpiringTest {
    private Expiring<Object> expiringObject;
    private Object object;

    @Before
    public void setUp () {
        expiringObject = new Expiring<>(object, Duration.ofMinutes(1));
    }

    @Test
    public void value () {
        assertEquals(object, expiringObject.value());
    }

    @Test
    public void isExpired () {
        expiringObject = new Expiring<>(object, Duration.ZERO);
        await().atLeast(1, TimeUnit.MILLISECONDS);
        assertTrue(expiringObject.isExpired());
        assertNull(expiringObject.value());
    }

    @Test
    public void expire () {
        expiringObject = new Expiring<>(object, Duration.ofDays(1));
        expiringObject.expire();
        assertNull(expiringObject.value());
    }
}