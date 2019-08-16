package com.thales.chaos.experiment.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.experiment.Experiment;

import java.time.Duration;

import static com.thales.chaos.constants.ExperimentConstants.*;

public class GenericContainerExperiment extends Experiment {
    public GenericContainerExperiment (Container container) {
        super(container);
    }

    public static GenericContainerExperimentBuilder builder () {
        return GenericContainerExperimentBuilder.builder();
    }

    public static final class GenericContainerExperimentBuilder {
        protected Container container;
        private Duration minimumDuration = Duration.ofSeconds(DEFAULT_EXPERIMENT_MINIMUM_DURATION_SECONDS);
        private Duration maximumDuration = Duration.ofMinutes(DEFAULT_EXPERIMENT_DURATION_MINUTES);
        private Duration finalizationDuration = Duration.ofSeconds(DEFAULT_TIME_BEFORE_FINALIZATION_SECONDS);
        private String specificExperiment;

        private GenericContainerExperimentBuilder () {
        }

        public static GenericContainerExperimentBuilder builder () {
            return new GenericContainerExperimentBuilder();
        }

        public GenericContainerExperimentBuilder withContainer (Container container) {
            this.container = container;
            return this;
        }

        public GenericContainerExperimentBuilder withDuration (Duration maximumDuration) {
            this.maximumDuration = maximumDuration;
            return this;
        }

        public GenericContainerExperimentBuilder withMinimumDuration (Duration minimumDuration) {
            this.minimumDuration = minimumDuration;
            return this;
        }

        public GenericContainerExperimentBuilder withFinalzationDuration (Duration finalizationDuration) {
            this.finalizationDuration = finalizationDuration;
            return this;
        }

        public GenericContainerExperimentBuilder withSpecificExperiment (String specificExperiment) {
            this.specificExperiment = specificExperiment;
            return this;
        }

        public GenericContainerExperiment build () {
            GenericContainerExperiment genericContainerExperiment = new GenericContainerExperiment(this.container);
            genericContainerExperiment.maximumDuration = this.maximumDuration;
            genericContainerExperiment.minimumDuration = this.minimumDuration;
            genericContainerExperiment.finalizationDuration = this.finalizationDuration;
            genericContainerExperiment.specificExperiment = this.specificExperiment;
            return genericContainerExperiment;
        }
    }
}
