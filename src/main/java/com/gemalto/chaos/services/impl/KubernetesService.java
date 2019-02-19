package com.gemalto.chaos.services.impl;

import com.gemalto.chaos.services.CloudService;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
