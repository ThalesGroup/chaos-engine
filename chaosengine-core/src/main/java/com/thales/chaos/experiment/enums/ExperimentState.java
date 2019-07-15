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
