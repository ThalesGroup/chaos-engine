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

package com.thales.chaos.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.services.compute.ComputeScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.compute.v1.*;
import com.thales.chaos.services.CloudService;
import com.thales.chaos.shellclient.ssh.GcpRuntimeSSHKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
@ConfigurationProperties("gcp.compute")
@ConditionalOnProperty("gcp.compute")
public class GcpComputeService implements CloudService {
    public static final String COMPUTE_CREDENTIALS = "compute-credentials";
    public static final String COMPUTE_PROJECT = "compute-project";
    private static final Logger log = LoggerFactory.getLogger(GcpComputeService.class);
    private String projectId;
    private String jsonKey;

    public void setProjectId (String projectId) {
        this.projectId = projectId;
    }

    public void setJsonKey (String jsonKey) {
        this.jsonKey = jsonKey;
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceClient instanceClient (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials credentials) throws IOException {
        return createInstanceClient(credentials);
    }

    private static InstanceClient createInstanceClient (GoogleCredentials googleCredentials) throws IOException {
        InstanceSettings settings = InstanceSettings.newBuilder()
                                                    .setCredentialsProvider(() -> googleCredentials)
                                                    .build();
        return InstanceClient.create(settings);
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceClient instanceClientUsingCommonCredentials (GoogleCredentials googleCredentials) throws IOException {
        log.info("Creating Google Compute Instance client using shared credentials");
        return createInstanceClient(googleCredentials);
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceGroupSettings instanceGroupSettings (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials googleCredentials) throws IOException {
        return InstanceGroupSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceGroupSettings instanceGroupSettingsUsingCommonCredentials (GoogleCredentials googleCredentials) throws IOException {
        return InstanceGroupSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    public InstanceGroupClient instanceGroupClient (InstanceGroupSettings instanceGroupSettings) throws IOException {
        return InstanceGroupClient.create(instanceGroupSettings);
    }

    @Bean
    public InstanceGroupManagerClient instanceGroupManagerClient (InstanceGroupManagerSettings instanceGroupManagerSettings) throws IOException {
        return InstanceGroupManagerClient.create(instanceGroupManagerSettings);
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceGroupManagerSettings instanceGroupManagerSettings (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials googleCredentials) throws IOException {
        return InstanceGroupManagerSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public InstanceGroupManagerSettings instanceGroupManagerSettingsUsingCommonCredentials (GoogleCredentials googleCredentials) throws IOException {
        return InstanceGroupManagerSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public RegionInstanceGroupSettings regionInstanceGroupSettings (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials googleCredentials) throws IOException {
        return RegionInstanceGroupSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public RegionInstanceGroupSettings regionInstanceGroupSettingsUsingCommonCredentials (GoogleCredentials googleCredentials) throws IOException {
        return RegionInstanceGroupSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    public RegionInstanceGroupClient regionInstanceGroupClient (RegionInstanceGroupSettings regionInstanceGroupSettings) throws IOException {
        return RegionInstanceGroupClient.create(regionInstanceGroupSettings);
    }

    @Bean
    public RegionInstanceGroupManagerClient regionInstanceGroupManagerClient (RegionInstanceGroupManagerSettings regionInstanceGroupManagerSettings) throws IOException {
        return RegionInstanceGroupManagerClient.create(regionInstanceGroupManagerSettings);
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public RegionInstanceGroupManagerSettings regionInstanceGroupManagerSettings (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials googleCredentials) throws IOException {
        return RegionInstanceGroupManagerSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public RegionInstanceGroupManagerSettings regionInstanceGroupManagerSettingsUsingCommonCredentials (
            GoogleCredentials googleCredentials) throws IOException {
        return RegionInstanceGroupManagerSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public ZoneOperationSettings zoneOperationSettings (
            @Qualifier(COMPUTE_CREDENTIALS) GoogleCredentials googleCredentials) throws IOException {
        return ZoneOperationSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = COMPUTE_CREDENTIALS)
    public ZoneOperationSettings zoneOperationSettingsUsingCommonCredentials (GoogleCredentials googleCredentials) throws IOException {
        return ZoneOperationSettings.newBuilder().setCredentialsProvider(() -> googleCredentials).build();
    }

    @Bean
    public ZoneOperationClient zoneOperationClient (ZoneOperationSettings zoneOperationSettings) throws IOException {
        return ZoneOperationClient.create(zoneOperationSettings);
    }

    @Bean(COMPUTE_CREDENTIALS)
    @JsonIgnore
    public GoogleCredentials googleCredentials () throws IOException {
        return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(jsonKey.getBytes()))
                                        .createScoped(ComputeScopes.CLOUD_PLATFORM);
    }

    @Bean(COMPUTE_PROJECT)
    public ProjectName googleProject () {
        return ProjectName.of(projectId);
    }

    @Bean
    public GcpRuntimeSSHKey sshKey () {
        return new GcpRuntimeSSHKey();
    }
}
