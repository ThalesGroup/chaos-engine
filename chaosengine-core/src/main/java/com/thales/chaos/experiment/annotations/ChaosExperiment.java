package com.thales.chaos.experiment.annotations;

import com.thales.chaos.experiment.enums.ExperimentType;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ChaosExperiment {
    @NotNull ExperimentType experimentType () default ExperimentType.STATE;

    @NotNull int minimumDurationInSeconds () default 30;

    @NotNull int maximumDurationInSeconds () default 300;

    @NotNull boolean cattleOnly () default false;
}
