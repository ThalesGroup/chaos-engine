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

package com.thales.chaos.experiment.enums;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ExperimentState {
    CREATED(0),
    STARTING(1),
    STARTED(2),
    SELF_HEALING(3),
    FINALIZING(4),
    FINISHED(5),
    FAILED(6),
    ;
    private static final Map<Integer, ExperimentState> EXPERIMENT_STATE_MAP;
    private static final EnumSet<ExperimentState> completedStates;

    /*
    Create a map of Integer to Experiment States, for use in ensuring they are continuous.
     */
    static {
        EXPERIMENT_STATE_MAP = Collections.unmodifiableMap(Arrays.stream(values())
                                                                 .collect(Collectors.toMap(ExperimentState::getLevel, Function
                                                                         .identity())));
    }

    /*
    Create an EnumSet of states where we consider the experiment complete and no longer need to evaluate it
     */
    static {
        completedStates = EnumSet.of(FINISHED, FAILED);
    }

    /*
    All Experiment States should have a level that is n+1 of the previous state. These levels should be continuous
    (i.e., no jumps in level unnecessarily).
     */
    private int level;

    ExperimentState (int level) {
        this.level = level;
    }

    public boolean isComplete () {
        return completedStates.contains(this);
    }

    @JsonIgnore
    public ExperimentState getNextLevel () {
        return EXPERIMENT_STATE_MAP.get(getLevel() + 1);
    }

    public Integer getLevel () {
        return level;
    }
}
