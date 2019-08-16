package com.thales.chaos.util;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CollectionUtilsTest {
    @Test
    public void getRandomItemFromCollection () {
        assertNull(CollectionUtils.getRandomItemFromCollection(Collections.emptySet()));
        assertThat(CollectionUtils.getRandomItemFromCollection(Set.of("abc", "123")), anyOf(is("abc"), is("123")));
        assertThat(CollectionUtils.getRandomItemFromCollection(List.of("abc", "123")), anyOf(is("abc"), is("123")));
        try {
            CollectionUtils.getRandomItemFromCollection(null);
            fail("Null Pointer expected");
        } catch (NullPointerException ignored) {
        }
    }
}