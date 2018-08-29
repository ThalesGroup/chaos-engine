package com.gemalto.chaos.services.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.gemalto.chaos.services.CloudService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.rds")
@ConditionalOnProperty({ "aws.rds.accessKeyId", "aws.rds.secretAccessKey" })
public class AwsRDSService implements CloudService {
    private String accessKeyId;
    private String secretAccessKey;
    private String region = "us-east-2";

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
    AWSStaticCredentialsProvider rdsCredentials () {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey));
    }

    @Bean
    @RefreshScope
    AmazonRDS amazonRDS (@Qualifier("rdsCredentials") AWSCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonRDSClientBuilder.standard()
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .withRegion(region)
                                     .build();
    }
}
