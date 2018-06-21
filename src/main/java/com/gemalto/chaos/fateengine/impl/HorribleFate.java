package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.springframework.stereotype.Component;

@Component
public class HorribleFate extends FateEngine {
    public HorribleFate () {
        minTimeToLive = 5;
        maxTimeToLive = 10;
        fateWeight = 1;
    }

    @Override
    public boolean canDestroy () {
        return true;
    }
}
