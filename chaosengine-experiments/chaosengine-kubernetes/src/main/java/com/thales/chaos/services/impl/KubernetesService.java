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

package com.thales.chaos.services.impl;

import com.thales.chaos.services.CloudService;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "kubernetes")
@ConditionalOnProperty({ "kubernetes.url", "kubernetes.token" })
public class KubernetesService implements CloudService {
    private String url;
    private String token;
    private Boolean validateSSL = false;
    private Boolean debug = false;

    public void setUrl (String url) {
        this.url = url;
    }

    public void setToken (String token) {
        this.token = token;
    }

    public void setValidateSSL (Boolean validateSSL) {
        this.validateSSL = validateSSL;
    }

    public void setDebug (Boolean debug) {
        this.debug = debug;
    }

    @Bean
    @RefreshScope
    ApiClient apiClient () {
        ApiClient apiClient = Config.fromToken(url, token, validateSSL);
        apiClient.setDebugging(debug);
        apiClient.setHttpClient(apiClient.getHttpClient()
                                         .newBuilder()
                                         .connectTimeout(Duration.ofMinutes(1))
                                         .readTimeout(Duration.ofMinutes(1))
                                         .build());
        return apiClient;
    }

    @Bean
    @RefreshScope
    CoreApi coreApi (ApiClient apiClient) {
        return new CoreApi(apiClient);
    }

    @Bean
    @RefreshScope
    CoreV1Api coreV1Api (ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    @RefreshScope
    Exec exec (ApiClient apiClient) {
        return new Exec(apiClient);
    }

    @Bean
    @RefreshScope
    AppsV1Api appsV1Api (ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }
}
