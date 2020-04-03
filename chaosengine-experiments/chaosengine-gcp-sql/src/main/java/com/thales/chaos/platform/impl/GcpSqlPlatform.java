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
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private static final String INSTANCE_RUNNING = "RUNNABLE";

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
        try {
            getInstances().stream().filter(Objects::nonNull).filter(this::isReady).forEach(i -> {
                System.out.println(i.getName());
                System.out.println(i.getKind());
                System.out.println("HA " + hasFailoverReplica(i));
                System.out.println("Replicas " + hasReadReplicas(i));
                System.out.println(i.getBackendType());
                System.out.println("====");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private boolean isReady (DatabaseInstance databaseInstance) {
        return INSTANCE_RUNNING.equals(databaseInstance.getState());
    }

    private boolean hasFailoverReplica (DatabaseInstance databaseInstance) {
        return databaseInstance.getFailoverReplica() != null;
    }

    private List<String> hasReadReplicas (DatabaseInstance databaseInstance) {
        return databaseInstance.getReplicaNames();
    }

    @Override
    public boolean isContainerRecycled (Container container) {
        return false;
    }

    List<DatabaseInstance> getInstances () throws IOException {
        return getSQLAdmin().instances().list(gcpCredentialsMetadata.getProjectId()).execute().getItems();
    }

    SQLAdmin getSQLAdmin () {
        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            return new SQLAdmin.Builder(httpTransport, jsonFactory, requestInitializer).setServicePath("sql/v1beta4/")
                                                                                       .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new ChaosException("GCP_SQL_GENERIC_ERROR", e);
        }
    }
}
