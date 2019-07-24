package com.thales.chaos.container.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Identifier {
    int order () default Integer.MAX_VALUE;
}
