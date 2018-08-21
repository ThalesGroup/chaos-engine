package com.gemalto.chaos.services.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.gemalto.chaos.services.CloudService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConditionalOnProperty({ "aws.ec2.accessKeyId", "aws.ec2.secretAccessKey" })
public class AwsService implements CloudService {
    @Value("${aws.ec2.accessKeyId}")
    private String accessKeyId;
    @Value("${aws.ec2.secretAccessKey}")
    private String secretAccessKey;
    @Value("${aws.ec2.region}")
    private String region;

    @Bean
    @RefreshScope
    AWSStaticCredentialsProvider awsStaticCredentialsProvider () {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey));
    }

    @Bean
    @RefreshScope
    AmazonEC2 amazonEC2 (AWSStaticCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonEC2ClientBuilder.standard()
                                     .withCredentials(awsStaticCredentialsProvider)
                                     .withRegion(region)
                                     .build();
    }
}
