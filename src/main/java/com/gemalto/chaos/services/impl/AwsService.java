package com.gemalto.chaos.services.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws")
@ConditionalOnProperty({ "aws.accessKeyId", "aws.secretAccessKey", "aws.region" })
public class AwsService {
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
    AWSStaticCredentialsProvider awsStaticCredentialsProvider () {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey));
    }

    @Bean
    @RefreshScope
    AmazonRDS amazonRDS (AWSStaticCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonRDSClientBuilder.standard()
                                     .withRegion(region)
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .build();
    }

    @Bean
    @RefreshScope
    AmazonEC2 amazonEC2 (AWSCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonEC2ClientBuilder.standard()
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .withRegion(region)
                                     .build();
    }
}
