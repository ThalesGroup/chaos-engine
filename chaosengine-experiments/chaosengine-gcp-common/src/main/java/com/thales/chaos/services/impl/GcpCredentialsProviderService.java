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
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.services.compute.ComputeScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.thales.chaos.services.CloudService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
@ConfigurationProperties("gcp")
@ConditionalOnProperty("gcp.json-key")
public class GcpCredentialsProviderService implements CloudService {
    public static final String GCP_CREDENTIALS = "gcp-credentials";
    private String jsonKey;

    public void setJsonKey (String jsonKey) {
        this.jsonKey = jsonKey;
    }

    @Bean(GCP_CREDENTIALS)
    @RefreshScope
    @JsonIgnore
    public GoogleCredentials googleCredentials () throws IOException {
        return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(jsonKey.getBytes()))
                                        .createScoped(ComputeScopes.CLOUD_PLATFORM);
    }

    @Bean
    @RefreshScope
    @JsonIgnore
    @ConditionalOnBean(value = GoogleCredentials.class, name = GCP_CREDENTIALS)
    public CredentialsProvider computeCredentialsProvider (@Qualifier(GCP_CREDENTIALS) GoogleCredentials credentials) {
        return () -> credentials;
    }

    @Bean
    @RefreshScope
    @JsonIgnore
    @ConditionalOnMissingBean(value = GoogleCredentials.class, name = GCP_CREDENTIALS)
    public CredentialsProvider genericCredentialsProvider (GoogleCredentials credentials) {
        return () -> credentials;
    }
}
