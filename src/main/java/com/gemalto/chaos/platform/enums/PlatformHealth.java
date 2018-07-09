package com.gemalto.chaos.platform.enums;

public enum PlatformHealth {
    OK(0),
    DEGRADED(1),
    FAILED(2);
    private Integer healthLevel;

    PlatformHealth (Integer level) {
        this.healthLevel = level;
    }

    public Integer getHealthLevel () {
        return healthLevel;
    }
}
