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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.thales.chaos.services.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@ConfigurationProperties(prefix = "aws")
@ConditionalOnProperty({ "aws.accessKeyId", "aws.secretAccessKey", "aws.region" })
public class AwsEC2Service implements CloudService {
    private static final Logger log = LoggerFactory.getLogger(AwsEC2Service.class);
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    public void setAccessKeyId (String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSecretAccessKey (String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public void setRegion (String region) {
        this.region = region;
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    AWSStaticCredentialsProvider awsStaticCredentialsProvider () {
        log.info("Creating AWS Credentials Provider");
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey));
    }

    @Bean
    @RefreshScope
    @Order(Ordered.HIGHEST_PRECEDENCE)
    AmazonEC2 amazonEC2 (AWSCredentialsProvider awsStaticCredentialsProvider) {
        log.info("Creating AWS EC2 Client");
        return AmazonEC2ClientBuilder.standard().withCredentials(awsStaticCredentialsProvider).withRegion(region).build();
    }

    @Bean
    @RefreshScope
    AmazonAutoScaling amazonAutoScaling (AWSCredentialsProvider awsCredentialsProvider) {
        log.info("Creating AWS AutoScaling Client");
        return AmazonAutoScalingClientBuilder.standard().withCredentials(awsCredentialsProvider).withRegion(region).build();
    }
}
