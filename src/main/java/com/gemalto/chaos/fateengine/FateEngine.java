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
        return fateWeight != null ? fateWeight : 1;
    }

    public boolean canDestroy() {
        return false;
    }

}
