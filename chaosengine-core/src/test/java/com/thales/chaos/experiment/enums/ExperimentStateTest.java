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

package com.thales.chaos.experiment.enums;

import org.junit.Test;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ExperimentStateTest {
    @Test
    public void ExperimentStateTestImpl () {
        assertNotNull(ExperimentState.valueOf("FINISHED"));
        assertNotNull(ExperimentState.valueOf("STARTED"));
        assertNotNull(ExperimentState.valueOf("CREATED"));
        assertNotNull(ExperimentState.valueOf("FAILED"));
    }

    @Test
    public void sequentialTestLevels () {
        Collection<ExperimentState> allTestLevels = Stream.iterate(ExperimentState.CREATED, ExperimentState::getNextLevel)
                                                          .takeWhile(Objects::nonNull)
                                                          .collect(Collectors.toList());
        assertThat("Experiment State levels should be continuous", allTestLevels, containsInAnyOrder(ExperimentState.values()));
    }
}