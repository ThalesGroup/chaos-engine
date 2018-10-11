package com.gemalto.chaos.experiment.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.Experiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;

import java.time.Duration;

import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_ATTACK_DURATION_MINUTES;
import static com.gemalto.chaos.constants.AttackConstants.DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS;

public class GenericContainerExperiment extends Experiment {
    public static GenericContainerExperimentBuilder builder () {
        return GenericContainerExperimentBuilder.builder();
    }

    public static final class GenericContainerExperimentBuilder {
        protected Container container;
        private ExperimentType experimentType;
        private Duration duration = Duration.ofMinutes(DEFAULT_ATTACK_DURATION_MINUTES);
        private Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);

        private GenericContainerExperimentBuilder () {
        }

        public static GenericContainerExperimentBuilder builder () {
            return new GenericContainerExperimentBuilder();
        }

        public GenericContainerExperimentBuilder withContainer (Container container) {
            this.container = container;
            return this;
        }

        public GenericContainerExperimentBuilder withExperimentType (ExperimentType experimentType) {
            this.experimentType = experimentType;
            return this;
        }

        public GenericContainerExperimentBuilder withDuration (Duration duration) {
            this.duration = duration;
            return this;
        }

        public GenericContainerExperimentBuilder withFinalzationDuration (Duration finalizationDuration) {
            this.finalizationDuration = finalizationDuration;
            return this;
        }

        public GenericContainerExperiment build () {
            GenericContainerExperiment genericContainerExperiment = new GenericContainerExperiment();
            genericContainerExperiment.experimentType = this.experimentType;
            genericContainerExperiment.container = this.container;
            genericContainerExperiment.duration = this.duration;
            genericContainerExperiment.finalizationDuration = this.finalizationDuration;
            return genericContainerExperiment;
        }
    }
}
