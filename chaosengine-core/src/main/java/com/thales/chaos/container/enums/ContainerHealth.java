package com.thales.chaos.container.enums;

public enum ContainerHealth {
    NORMAL(0),
    RUNNING_EXPERIMENT(1),
    DOES_NOT_EXIST(2);
    private int healthLevel;

    ContainerHealth (int healthLevel) {
        this.healthLevel = healthLevel;
    }

    public int getHealthLevel () {
        return healthLevel;
    }
}
