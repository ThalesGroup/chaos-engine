package com.thales.chaos.util;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CollectionUtils {
    private static final Random RANDOM = new Random();

    private CollectionUtils () {
    }

    public static <T> T getRandomItemFromCollection (@NotNull Collection<T> collection) {
        return getRandomItemFromList(collection instanceof List ? (List<T>) collection : List.copyOf(collection));
    }

    private static <T> T getRandomItemFromList (@NotNull List<T> list) {
        Objects.requireNonNull(list);
        if (list.isEmpty()) return null;
        return list.get(RANDOM.nextInt(list.size()));
    }
}
