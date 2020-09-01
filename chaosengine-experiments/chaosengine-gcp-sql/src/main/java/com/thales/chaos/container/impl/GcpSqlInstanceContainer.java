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

package com.thales.chaos.container.impl;

import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentScope;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.platform.impl.GcpSqlPlatform;

import java.io.IOException;

public class GcpSqlInstanceContainer extends GcpSqlContainer {
    @ChaosExperiment(experimentType = ExperimentType.STATE, experimentScope = ExperimentScope.PET)
    public void restartInstance (Experiment experiment) throws IOException {
        String operationName = platform.restartInstance(this.getName());
        experiment.setCheckContainerHealth(() -> platform.isOperationComplete(operationName) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT);
    }

    public static GcpSqlInstanceContainerBuilder builder () {
        return new GcpSqlInstanceContainerBuilder();
    }

    public static class GcpSqlInstanceContainerBuilder {
        private String name;
        private GcpSqlPlatform platform;

        private GcpSqlInstanceContainerBuilder () {
        }

        public GcpSqlInstanceContainerBuilder withName (String name) {
            this.name = name;
            return this;
        }

        public GcpSqlInstanceContainerBuilder withPlatform (GcpSqlPlatform platform) {
            this.platform = platform;
            return this;
        }

        public GcpSqlInstanceContainer build () {
            GcpSqlInstanceContainer gcpSqlInstanceContainer = new GcpSqlInstanceContainer();
            gcpSqlInstanceContainer.name = this.name;
            gcpSqlInstanceContainer.platform = this.platform;
            return gcpSqlInstanceContainer;
        }
    }
}
