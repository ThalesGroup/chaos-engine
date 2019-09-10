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

package com.thales.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import com.thales.chaos.platform.Platform;

import java.util.*;
import java.util.stream.Collectors;

@JsonPropertyOrder({ ExperimentSuite.PLATFORM_TYPE_KEY, ExperimentSuite.EXPERIMENT_CRITERIA })
public class ExperimentSuite {
    public static final String PLATFORM_TYPE_KEY = "platformType";
    public static final String EXPERIMENT_CRITERIA = "experimentCriteria";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @JsonProperty(required = true, value = PLATFORM_TYPE_KEY)
    private String platformType;
    @JsonProperty(required = true, value = EXPERIMENT_CRITERIA)
    private Collection<ExperimentCriteria> experimentCriteria;

    @JsonCreator
    ExperimentSuite (@JsonProperty(required = true, value = PLATFORM_TYPE_KEY) String platformType, @JsonProperty(required = true, value = EXPERIMENT_CRITERIA) Collection<ExperimentCriteria> experimentCriteria) {
        this.platformType = platformType;
        this.experimentCriteria = experimentCriteria;
    }

    static ExperimentSuite fromExperiments (Platform platform, Collection<Experiment> experiments) {
        Map<String, List<String>> map = experiments.stream()
                                                   .collect(Collectors.groupingBy(experiment -> experiment.getContainer()
                                                                                                          .getAggregationIdentifier(), Collectors
                                                           .mapping(Experiment::getExperimentMethodName, Collectors.toList())));
        return new ExperimentSuite(platform.getPlatformType(), fromMap(map));
    }

    static Set<ExperimentCriteria> fromMap (Map<String, List<String>> map) {
        return map.entrySet().stream().map(ExperimentSuite::fromMapEntry).collect(Collectors.toUnmodifiableSet());
    }

    private static ExperimentCriteria fromMapEntry (Map.Entry<String, List<String>> mapEntry) {
        return new ExperimentCriteria(mapEntry.getKey(), mapEntry.getValue());
    }

    Collection<ExperimentCriteria> getExperimentCriteria () {
        return experimentCriteria;
    }

    String getPlatformType () {
        return platformType;
    }

    @Override
    public String toString () {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new ChaosException(ChaosErrorCode.OBJECT_SERIALIZATION_ERROR, e);
        }
    }

    @Override
    public int hashCode () {
        int result = platformType.hashCode();
        result = 31 * result + experimentCriteria.hashCode();
        return result;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentSuite that = (ExperimentSuite) o;
        if (!platformType.equals(that.platformType)) return false;
        return experimentCriteria.containsAll(that.experimentCriteria);
    }

    public static class ExperimentCriteria {
        @JsonProperty(required = true, value = "containerIdentifier")
        private final String containerIdentifier;
        @JsonProperty(required = true, value = "experimentMethods")
        private List<String> experimentMethods;
        @JsonProperty(defaultValue = "[]", value = "specificContainerTargets")
        private List<String> specificContainerTargets;

        ExperimentCriteria (String containerIdentifier, List<String> experimentMethods) {
            this(containerIdentifier, experimentMethods, Collections.emptyList());
        }

        @JsonCreator
        ExperimentCriteria (@JsonProperty(required = true, value = "containerIdentifier") String containerIdentifier, @JsonProperty(required = true, value = "experimentMethods") List<String> experimentMethods, @JsonProperty(defaultValue = "[]", value = "specificContainerTargets") List<String> specificContainerTargets) {
            this.containerIdentifier = containerIdentifier;
            this.experimentMethods = experimentMethods;
            this.specificContainerTargets = specificContainerTargets;
            if (specificContainerTargets.size() > experimentMethods.size()) {
                throw new IllegalArgumentException("Experiment Methods should be at least the same size as Specific Container Targets");
            }
        }

        String getContainerIdentifier () {
            return containerIdentifier;
        }

        List<String> getExperimentMethods () {
            return experimentMethods;
        }

        List<String> getSpecificContainerTargets () {
            return specificContainerTargets;
        }

        @Override
        public int hashCode () {
            int result = containerIdentifier.hashCode();
            result = 31 * result + experimentMethods.hashCode();
            result = 31 * result + specificContainerTargets.hashCode();
            return result;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExperimentCriteria that = (ExperimentCriteria) o;
            if (!containerIdentifier.equals(that.containerIdentifier)) return false;
            if (!experimentMethods.equals(that.experimentMethods)) return false;
            return specificContainerTargets.equals(that.specificContainerTargets);
        }
    }
}
