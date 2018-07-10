package com.gemalto.chaos.attack;

public interface AttackableObject {
    default boolean canAttack () {
        return false;
    }
}
