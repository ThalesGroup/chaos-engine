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

package com.thales.chaos.scripts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.experiment.enums.ExperimentType;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.Collection;

public interface Script {
    String getHealthCheckCommand ();

    String getSelfHealingCommand ();

    boolean isRequiresCattle ();

    String getScriptName ();

    boolean doesNotUseMissingDependencies (Collection<String> knownMissingDependencies);

    String getFinalizeCommand ();

    @JsonIgnore
    Resource getResource ();

    ExperimentType getExperimentType ();

    Collection<String> getDependencies ();

    Duration getMaximumDuration ();

    Duration getMinimumDuration ();
}
