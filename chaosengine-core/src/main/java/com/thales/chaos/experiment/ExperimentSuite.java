package com.thales.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import com.thales.chaos.platform.Platform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonPropertyOrder({ ExperimentSuite.PLATFORM_TYPE_KEY, ExperimentSuite.AGGREGATION_IDENTIFIER_EXPERIMENT_METHOD_MAP_KEY })
class ExperimentSuite {
    public static final String PLATFORM_TYPE_KEY = "platformType";
    public static final String AGGREGATION_IDENTIFIER_EXPERIMENT_METHOD_MAP_KEY = "aggregationIdentifierToExperimentMethodsMap";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @JsonProperty(required = true, value = PLATFORM_TYPE_KEY)
    private String platformType;
    @JsonProperty(required = true, value = AGGREGATION_IDENTIFIER_EXPERIMENT_METHOD_MAP_KEY)
    private Map<String, List<String>> aggregationIdentifierToExperimentMethodsMap;

    public ExperimentSuite (String platformType, Map<String, List<String>> aggregationIdentifierToExperimentMethodsMap) {
        this.platformType = platformType;
        this.aggregationIdentifierToExperimentMethodsMap = aggregationIdentifierToExperimentMethodsMap;
    }

    String getPlatformType () {
        return platformType;
    }

    Map<String, List<String>> getAggregationIdentifierToExperimentMethodsMap () {
        return aggregationIdentifierToExperimentMethodsMap;
    }

    @Override
    public int hashCode () {
        int result = platformType != null ? platformType.hashCode() : 0;
        result = 31 * result + (aggregationIdentifierToExperimentMethodsMap != null ? aggregationIdentifierToExperimentMethodsMap
                .hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentSuite that = (ExperimentSuite) o;
        if (!Objects.equals(platformType, that.platformType)) return false;
        return Objects.equals(aggregationIdentifierToExperimentMethodsMap, that.aggregationIdentifierToExperimentMethodsMap);
    }

    static ExperimentSuite fromExperiments (Platform platform, Collection<Experiment> experiments) {
        Map<String, List<String>> map = experiments.stream()
                                                   .collect(Collectors.groupingBy(experiment -> experiment.getContainer()
                                                                                                          .getAggregationIdentifier(), Collectors
                                                           .mapping(Experiment::getExperimentMethodName, Collectors.toList())));
        return new ExperimentSuite(platform.getPlatformType(), map);
    }

    @Override
    public String toString () {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new ChaosException(ChaosErrorCode.OBJECT_SERIALIZATION_ERROR, e);
        }
    }
}
