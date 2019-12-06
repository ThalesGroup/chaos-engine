/*
 *    Copyright (c) 2019 Thales Group
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