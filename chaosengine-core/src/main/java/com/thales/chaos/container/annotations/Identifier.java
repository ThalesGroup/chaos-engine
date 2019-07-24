package com.thales.chaos.container.annotations;

public @interface Identifier {
    int order () default Integer.MAX_VALUE;
}
