package com.gemalto.chaos.services.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.gemalto.chaos.services.CloudService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.ec2")
@ConditionalOnProperty({ "aws.ec2.accessKeyId", "aws.ec2.secretAccessKey" })
public class AwsEC2Service implements CloudService {
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
    AWSStaticCredentialsProvider ec2Credentials () {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey));
    }

    @Bean
    @RefreshScope
    AmazonEC2 amazonEC2 (@Qualifier("ec2Credentials") AWSStaticCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonEC2ClientBuilder.standard()
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .withRegion(region)
                                     .build();
    }
}
