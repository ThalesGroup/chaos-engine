/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
