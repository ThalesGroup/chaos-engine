package com.thales.chaos.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import static java.time.Instant.now;

public class Expiring<T> {
    private T object;
    private Instant expiryTime;
    private boolean expired;

    public Expiring (T object, Instant expiryTime) {
        this.object = object;
        this.expiryTime = expiryTime;
    }

    public Expiring (T object, long millis) {
        this(object, Duration.ofMillis(millis));
    }

    public Expiring (T object, Duration objectDuration) {
        this.object = object;
        this.expiryTime = now().plus(objectDuration);
    }

    public T value () {
        if (isExpired()) return null;
        return this.object;
    }

    @Override
    public String toString () {
        return String.format("%s (%s left)", this.object, Duration.between(now(), expiryTime));
    }

    Instant getExpiryTime () {
        return expiryTime;
    }

    public void expire () {
        expired = true;
    }

    public T computeIfAbsent (Callable<T> callable, long durationInMillis) {
        return computeIfAbsent(callable, Duration.ofMillis(durationInMillis));
    }

    public T computeIfAbsent (Callable<T> callable, Duration objectDuration) {
        if (isExpired()) {
            try {
                this.object = callable.call();
                this.expiryTime = now().plus(objectDuration);
                this.expired = false;
            } catch (Exception ignored) {
                return null;
            }
        }
        return this.object;
    }

    boolean isExpired () {
        Instant now = now();
        if (expired) {
            return true;
        } else if (now.isAfter(expiryTime)) {
            expired = true;
            return true;
        }
        return false;
    }
}
