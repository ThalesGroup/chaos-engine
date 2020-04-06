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
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.impl.GcpSqlPlatform;

import javax.validation.constraints.NotNull;

public class GcpSqlClusterContainer extends GcpSqlContainer {
    public static GcpSqlClusterContainerBuilder builder () {
        return new GcpSqlClusterContainerBuilder();
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return null;
    }

    @Override
    public String getSimpleName () {
        return null;
    }

    @Override
    public String getAggregationIdentifier () {
        return null;
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return null;
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return false;
    }

    public static class GcpSqlClusterContainerBuilder {
        private String name;
        private GcpSqlPlatform platform;

        private GcpSqlClusterContainerBuilder () {
        }

        public GcpSqlClusterContainerBuilder withName (String name) {
            this.name = name;
            return this;
        }

        public GcpSqlClusterContainerBuilder withPlatform (GcpSqlPlatform platform) {
            this.platform = platform;
            return this;
        }

        public GcpSqlClusterContainer build () {
            GcpSqlClusterContainer gcpSqlClusterContainer = new GcpSqlClusterContainer();
            gcpSqlClusterContainer.name = this.name;
            gcpSqlClusterContainer.platform = this.platform;
            return gcpSqlClusterContainer;
        }
    }
}