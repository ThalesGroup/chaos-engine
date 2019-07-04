package com.thales.chaos.experiment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class ExperimentSuite {
    @JsonProperty(required = true)
    private String platformType;
    @JsonProperty(required = true)
    private Map<String, List<String>> aggregationIdentifierToExperimentMethodsMap;

    public ExperimentSuite () {
    }

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
}
