package com.gemalto.chaos.attack.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum AttackType {
    RESOURCE,
    NETWORK,
    STATE;
    private static final List<AttackType> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    public static AttackType getRandom () {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }
}
