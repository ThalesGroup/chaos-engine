package com.gemalto.chaos.attack.enums;

import com.gemalto.chaos.attack.annotations.NetworkAttack;
import com.gemalto.chaos.attack.annotations.ResourceAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;

import java.lang.annotation.Annotation;

public enum AttackType {
    RESOURCE(ResourceAttack.class),
    NETWORK(NetworkAttack.class),
    STATE(StateAttack.class);
    private Class<? extends Annotation> annotation;

    AttackType (Class<? extends Annotation> associatedAnnotation) {
        this.annotation = associatedAnnotation;
    }

    public Class<? extends Annotation> getAnnotation () {
        return annotation;
    }
}
