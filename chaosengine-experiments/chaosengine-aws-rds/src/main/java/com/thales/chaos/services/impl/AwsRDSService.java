package com.thales.chaos.services.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConfigurationProperties(prefix = "aws")
@ConditionalOnProperty({ "aws.accessKeyId", "aws.secretAccessKey", "aws.region" })
public class AwsRDSService {
    private static final Logger log = LoggerFactory.getLogger(AwsRDSService.class);
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
    AmazonRDS amazonRDS (AWSStaticCredentialsProvider awsStaticCredentialsProvider) {
        log.info("Creating AWS RDS Client");
        return AmazonRDSClientBuilder.standard()
                                     .withRegion(region)
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .build();
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    @Order
    AmazonEC2 amazonEC2 (AWSCredentialsProvider awsStaticCredentialsProvider) {
        log.info("Creating AWS EC2 Client");
        return AmazonEC2ClientBuilder.standard()
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .withRegion(region)
                                     .build();
    }
}