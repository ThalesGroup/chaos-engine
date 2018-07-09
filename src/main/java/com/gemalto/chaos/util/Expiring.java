package com.gemalto.chaos.util;

import java.time.Duration;
import java.time.Instant;

public class Expiring<T> {
    private T object;
    private Instant expiryTime;

    public Expiring (T object, Instant expiryTime) {
        this.object = object;
        this.expiryTime = expiryTime;
    }

    public Expiring (T object, long millis) {
        this(object, Duration.ofMillis(millis));
    }

    public Expiring (T object, Duration objectDuration) {
        this.object = object;
        this.expiryTime = Instant.now().plus(objectDuration);
    }

    public T value () {
        if (isExpired()) return null;
        return this.object;
    }

    boolean isExpired () {
        return Instant.now().isAfter(expiryTime);
    }

    @Override
    public String toString () {
        return String.format("%s (%s left)", this.object, Duration.between(Instant.now(), expiryTime));
    }
}
