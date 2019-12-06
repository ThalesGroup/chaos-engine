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

package com.thales.chaos.container.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.impl.CloudFoundryContainerPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.logstash.logback.argument.StructuredArguments.v;

public class CloudFoundryContainer extends Container {
    @Identifier(order = 0)
    private String applicationId;
    @Identifier(order = 1)
    private String name;
    @Identifier(order = 2)
    private Integer instance;
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;
    private Runnable restageApplication = () -> cloudFoundryContainerPlatform.restageApplication(getRestageApplicationRequest());
    private Callable<ContainerHealth> isInstanceRunning = () -> cloudFoundryContainerPlatform.checkHealth(applicationId, instance);

    private CloudFoundryContainer () {
        super();
    }

    CloudFoundryContainer (String applicationId, String name, Integer instance) {
        super();
        this.applicationId = applicationId;
        this.name = name;
        this.instance = instance;
    }

    public static CloudFoundryContainerBuilder builder () {
        return CloudFoundryContainerBuilder.builder();
    }

    @Override
    public Platform getPlatform () {
        return cloudFoundryContainerPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
        return cloudFoundryContainerPlatform.checkHealth(applicationId, instance);
    }

    @Override
    public String getSimpleName () {
        return name + " - (" + instance + ")";
    }

    public String getName () {
        return getAggregationIdentifier();
    }

    @Override
    public DataDogIdentifier getDataDogIdentifier () {
        return DataDogIdentifier.dataDogIdentifier().withValue(name + "-" + instance);
    }

    @Override
    protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
        return uniqueIdentifier.equals(name + "-" + instance);
    }

    @Override
    public boolean isCattle () {
        return true;
    }

    @ChaosExperiment(experimentType = ExperimentType.STATE)
    public void restartContainer (Experiment experiment) {
        experiment.setSelfHealingMethod(restageApplication);
        experiment.setCheckContainerHealth(isInstanceRunning);
        cloudFoundryContainerPlatform.restartInstance(getRestartApplicationInstanceRequest());
    }

    private RestartApplicationInstanceRequest getRestartApplicationInstanceRequest () {
        RestartApplicationInstanceRequest restartApplicationInstanceRequest = RestartApplicationInstanceRequest.builder()
                                                                                                               .name(name)
                                                                                                               .instanceIndex(instance)
                                                                                                               .build();
        log.info("{}", v("restartApplicationInstanceRequest", restartApplicationInstanceRequest));
        return restartApplicationInstanceRequest;
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    public String getApplicationId () {
        return applicationId;
    }

    @Override
    public String getAggregationIdentifier () {
        return name;
    }

    public Integer getInstance () {
        return instance;
    }

    public static final class CloudFoundryContainerBuilder {
        private final Map<String, String> dataDogTags = new HashMap<>();
        private String applicationId;
        private String name;
        private Integer instance;
        private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;

        private CloudFoundryContainerBuilder () {
        }

        static CloudFoundryContainerBuilder builder () {
            return new CloudFoundryContainerBuilder();
        }

        public CloudFoundryContainerBuilder applicationId (String applicationId) {
            this.applicationId = applicationId;
            return dataDogTags("application_id", applicationId);
        }

        public CloudFoundryContainerBuilder dataDogTags (String key, String value) {
            this.dataDogTags.put(key, value);
            return this;
        }

        public CloudFoundryContainerBuilder name (String name) {
            this.name = name;
            return dataDogTags("application_name", name);
        }

        public CloudFoundryContainerBuilder platform (CloudFoundryContainerPlatform cloudFoundryContainerPlatform) {
            this.cloudFoundryContainerPlatform = cloudFoundryContainerPlatform;
            return this;
        }

        public CloudFoundryContainerBuilder instance (Integer instance) {
            this.instance = instance;
            return dataDogTags("instance_index", instance.toString());
        }

        public CloudFoundryContainer build () {
            CloudFoundryContainer cloudFoundryContainer = new CloudFoundryContainer();
            cloudFoundryContainer.name = this.name;
            cloudFoundryContainer.instance = this.instance;
            cloudFoundryContainer.applicationId = this.applicationId;
            cloudFoundryContainer.cloudFoundryContainerPlatform = this.cloudFoundryContainerPlatform;
            return cloudFoundryContainer;
        }
    }
}
