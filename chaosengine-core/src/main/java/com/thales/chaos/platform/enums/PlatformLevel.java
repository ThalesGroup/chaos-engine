package com.thales.chaos.platform.enums;

public enum PlatformLevel {
    OVERALL(-1),
    IAAS(0),
    PAAS(1),
    SAAS(2);
    private Integer level;

    PlatformLevel (Integer level) {
        this.level = level;
    }

    public Integer getLevel () {
        return level;
    }
}
