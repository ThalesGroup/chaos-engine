package com.gemalto.chaos.fateengine;

public abstract class FateEngine {

    protected Integer fateWeight;
    protected Integer minTimeToLive;
    protected Integer maxTimeToLive;

    Integer getMinTimeToLive() {
        return minTimeToLive != null ? minTimeToLive : 1;
    }

    Integer getMaxTimeToLive() {
        return maxTimeToLive != null ? minTimeToLive : 5;
    }

    Integer getFateWeight() {
        return fateWeight == null ? 1 : fateWeight;
    }

    public boolean canDestroy() {
        return false;
    }

}
