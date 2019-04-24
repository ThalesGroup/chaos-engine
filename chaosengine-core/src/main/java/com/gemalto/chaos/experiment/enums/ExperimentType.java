package com.gemalto.chaos.experiment.enums;

import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.ResourceExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public enum ExperimentType {
    RESOURCE(ResourceExperiment.class),
    NETWORK(NetworkExperiment.class),
    STATE(StateExperiment.class);
    private Class<? extends Annotation> annotation;

    ExperimentType (Class<? extends Annotation> associatedAnnotation) {
        this.annotation = associatedAnnotation;
    }

    public Class<? extends Annotation> getAnnotation () {
        return annotation;
    }

    public static boolean isExperiment (Annotation annotation) {
        return Arrays.stream(ExperimentType.values())
                     .map(ExperimentType::getAnnotation)
                     .anyMatch(c -> annotation.annotationType().equals(c));
    }

    public static ExperimentType valueOf (Annotation annotation) {
        return Arrays.stream(ExperimentType.values())
                     .filter(experimentType -> experimentType.getAnnotation().equals(annotation.annotationType()))
                     .findFirst()
                     .orElse(null);
    }
}
