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

package com.thales.chaos.platform.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.FailoverContext;
import com.google.api.services.sqladmin.model.InstancesFailoverRequest;
import com.google.api.services.sqladmin.model.Settings;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.GcpSqlClusterContainer;
import com.thales.chaos.container.impl.GcpSqlContainer;
import com.thales.chaos.container.impl.GcpSqlInstanceContainer;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.services.impl.GcpCredentialsMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static net.logstash.logback.argument.StructuredArguments.kv;

@ConditionalOnProperty("gcp.sql")
@ConfigurationProperties("gcp.sql")
@Component
public class GcpSqlPlatform extends Platform {
    @Autowired
    private GoogleCredentials googleCredentials;
    @Autowired
    private ContainerManager containerManager;
    @Autowired
    private GcpCredentialsMetadata gcpCredentialsMetadata;
    private Map<String, String> includeFilter = Collections.emptyMap();
    private Map<String, String> excludeFilter = Collections.emptyMap();
    static final String SQL_INSTANCE_RUNNING = "RUNNABLE";
    private static final String SQL_SERVICE_PATH = "sql/v1beta4/";
    static final String SQL_FAILOVER_CONTEXT = "sql#failoverContext";
    private static final String SQL_OPERATION_COMPLETE = "DONE";

    public GcpSqlPlatform () {
        log.info("GCP SQL Platform created");
    }

    @Override
    public ApiStatus getApiStatus () {
        try {
            getInstances();
        } catch (RuntimeException | IOException e) {
            log.error("Caught error when evaluating API Status of Google Cloud Platform", e);
            return ApiStatus.ERROR;
        }
        return ApiStatus.OK;
    }

    @Override
    public PlatformLevel getPlatformLevel () {
        return PlatformLevel.IAAS;
    }

    @Override
    public PlatformHealth getPlatformHealth () {
        try {
            return !getInstances().isEmpty() ? PlatformHealth.OK : PlatformHealth.DEGRADED;
        } catch (RuntimeException | IOException e) {
            log.error("GCP SQL Platform health check failed", e);
            return PlatformHealth.FAILED;
        }
    }

    @Override
    protected List<Container> generateRoster () {
        log.debug("Generating roster of GCP SQL instances");
        List<DatabaseInstance> masterInstances = getMasterInstances();
        return masterInstances.stream().map(this::createContainerFromInstance).collect(Collectors.toList());
    }

    List<DatabaseInstance> getMasterInstances () {
        try {
            return getInstances().stream()
                                 .filter(Objects::nonNull)
                                 .filter(this::isNotFiltered)
                                 .filter(this::isReady)
                                 .filter(this::isHA)
                                 .filter(not(this::isReadReplica))
                                 .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Cannot get master instances", e);
            return Collections.emptyList();
        }
    }

    List<DatabaseInstance> getReadReplicas (DatabaseInstance masterInstance) {
        try {
            return getInstances().stream()
                                 .filter(Objects::nonNull)
                                 .filter(this::isReadReplica)
                                 .filter(replica -> masterInstance.getName().equals(replica.getMasterInstanceName()))
                                 .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Cannot get read replicas", e);
            return Collections.emptyList();
        }
    }

    GcpSqlContainer createContainerFromInstance (DatabaseInstance masterInstance) {
        if (hasReadReplicas(masterInstance)) {
            log.debug("Creating new SQL cluster container {}", masterInstance.getName());
            return GcpSqlClusterContainer.builder().withName(masterInstance.getName()).withPlatform(this).build();
        }
        log.debug("Creating new SQL instance container {}", masterInstance.getName());
        return GcpSqlInstanceContainer.builder().withName(masterInstance.getName()).withPlatform(this).build();
    }

    private boolean isHA (DatabaseInstance databaseInstance) {
        return databaseInstance.getFailoverReplica() != null;
    }

    private boolean isReady (DatabaseInstance databaseInstance) {
        return SQL_INSTANCE_RUNNING.equals(databaseInstance.getState());
    }

    private boolean isReadReplica (DatabaseInstance databaseInstance) {
        return databaseInstance.getMasterInstanceName() != null;
    }

    public ContainerHealth isContainerRunning (GcpSqlContainer container) {
        DatabaseInstance masterInstance = getInstance(container.getName());
        if (masterInstance == null) {
            return ContainerHealth.DOES_NOT_EXIST;
        }
        return isReady(masterInstance) && replicasRunning(masterInstance) ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
    }

    public DatabaseInstance getInstance (String instanceID) {
        try {
            return getSQLAdmin().instances().get(gcpCredentialsMetadata.getProjectId(), instanceID).execute();
        } catch (IOException e) {
            log.error("Cannot get GCP SQL instance {}", instanceID, e);
            return null;
        }
    }

    boolean isNotFiltered (DatabaseInstance instance) {
        Collection<Map.Entry<String, String>> itemsList = Optional.of(instance)
                                                                  .map(DatabaseInstance::getSettings)
                                                                  .map(Settings::getUserLabels)
                                                                  .map(GcpSqlPlatform::asItemCollection)
                                                                  .orElse(emptySet());
        Collection<Map.Entry<String, String>> includeFilterItems = getIncludeFilter();
        Collection<Map.Entry<String, String>> excludeFilterItems = getExcludeFilter();
        boolean hasAllMustIncludes = includeFilter.isEmpty() || itemsList.stream()
                                                                         .anyMatch(includeFilterItems::contains);
        boolean hasNoMustNotIncludes = itemsList.stream().noneMatch(excludeFilterItems::contains);
        final boolean isNotFiltered = hasAllMustIncludes && hasNoMustNotIncludes;
        if (!isNotFiltered) {
            log.info("Instance {} filtered because of {}, {}",
                    instance.getName(),
                    kv("includeFilter", hasAllMustIncludes),
                    kv("excludeFilter", hasNoMustNotIncludes));
        }
        return isNotFiltered;
    }

    static Collection<Map.Entry<String, String>> asItemCollection (Map<String, String> itemMap) {
        return itemMap.entrySet();
    }

    public Collection<Map.Entry<String, String>> getIncludeFilter () {
        return asItemCollection(includeFilter);
    }

    public void setIncludeFilter (Map<String, String> includeFilter) {
        this.includeFilter = includeFilter;
    }

    public Collection<Map.Entry<String, String>> getExcludeFilter () {
        return asItemCollection(excludeFilter);
    }

    public void setExcludeFilter (Map<String, String> excludeFilter) {
        this.excludeFilter = excludeFilter;
    }

    private boolean replicasRunning (DatabaseInstance masterInstance) {
        return getReadReplicas(masterInstance).stream().allMatch(this::isReady);
    }

    private boolean hasReadReplicas (DatabaseInstance databaseInstance) {
        return databaseInstance.getReplicaNames() != null && !databaseInstance.getReplicaNames().isEmpty();
    }

    public String failover (GcpSqlContainer container) throws IOException {
        DatabaseInstance instance = getInstance(container.getName());
        InstancesFailoverRequest instancesFailoverRequest = new InstancesFailoverRequest();
        instancesFailoverRequest.setFailoverContext(new FailoverContext().setSettingsVersion(instance.getSettings()
                                                                                                     .getSettingsVersion())
                                                                         .setKind(SQL_FAILOVER_CONTEXT));
        log.debug("Failover triggered for {}", instance.getName());
        return getSQLAdmin().instances()
                            .failover(gcpCredentialsMetadata.getProjectId(),
                                    instance.getName(),
                                    instancesFailoverRequest)
                            .execute()
                            .getName();
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        throw new ChaosException("SQL_DOES_NOT_SUPPORT_RECYCLING");
    }

    List<DatabaseInstance> getInstances () throws IOException {
        return getSQLAdmin().instances().list(gcpCredentialsMetadata.getProjectId()).execute().getItems();
    }

    public boolean isOperationComplete (String operationName) {
        try {
            String status = getSQLAdmin().operations()
                                         .get(gcpCredentialsMetadata.getProjectId(), operationName)
                                         .execute()
                                         .getStatus();
            return SQL_OPERATION_COMPLETE.equals(status);
        } catch (IOException e) {
            log.error("Cannot fetch operation", e);
        }
        return false;
    }

    SQLAdmin getSQLAdmin () {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            return new SQLAdmin.Builder(httpTransport, jsonFactory, requestInitializer).setServicePath(SQL_SERVICE_PATH)
                                                                                       .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new ChaosException("GCP_SQL_GENERIC_ERROR", e);
        }
    }
}
