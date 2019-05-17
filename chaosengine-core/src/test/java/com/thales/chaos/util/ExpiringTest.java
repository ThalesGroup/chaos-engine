package com.thales.chaos.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExpiringTest {
    private Expiring<Object> expiringObject;
    private Object object = new Object();

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
        expiringObject = new Expiring<>(object, Duration.ofMillis(50));
        await().atLeast(100, TimeUnit.MILLISECONDS)
               .atMost(200, TimeUnit.MILLISECONDS)
               .until(() -> expiringObject.getExpiryTime().isBefore(Instant.now()));
        assertTrue(expiringObject.isExpired());
        assertNull(expiringObject.value());
    }

    @Test
    public void expire () {
        expiringObject = new Expiring<>(object, Duration.ofDays(1));
        expiringObject.expire();
        assertNull(expiringObject.value());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void computeIfAbsent () throws Exception {
        Object object2 = new Object();
        Callable<Object> callable = Mockito.spy(Callable.class);
        doReturn(object2).when(callable).call();
        expiringObject = new Expiring<>(object, Duration.ZERO);
        await().atLeast(100, TimeUnit.MILLISECONDS).until(() -> expiringObject.isExpired());
        assertEquals(object2, expiringObject.computeIfAbsent(callable, Duration.ZERO));
        verify(callable, atLeastOnce()).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void computeIfAbsentNotAbsent () throws Exception {
        Callable<Object> callable = Mockito.spy(Callable.class);
        assertEquals(object, expiringObject.computeIfAbsent(callable, Duration.ZERO));
        verify(callable, never()).call();
    }

    @Test
    public void testConstructor () {
        final Expiring<Object> t = new Expiring<>(object, Instant.now());
        await().atLeast(100, TimeUnit.MILLISECONDS).until(t::isExpired);
        assertNull(t.computeIfAbsent(() -> {
            throw new Exception();
        }, -1000L));
        final Expiring<Object> t2 = new Expiring<>(object, -1000L);
        await().atLeast(100, TimeUnit.MILLISECONDS).until(t2::isExpired);
        assertNull(t2.computeIfAbsent(() -> {
            throw new Exception();
        }, -1000L));
    }

    @Test
    public void testToString () {
        Instant expiryTime = Instant.now();
        Object to = new Object();
        Expiring<Object> t = new Expiring<>(to, expiryTime);
        assertTrue(t.toString().contains(String.format("%s", to)));
    }

}