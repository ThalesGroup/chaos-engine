package com.gemalto.chaos.services.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.gemalto.chaos.services.CloudService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({ "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY" })
public class AwsService implements CloudService {
    @Bean
    AWSStaticCredentialsProvider awsStaticCredentialsProvider (@Value("AWS_ACCESS_KEY_ID") String accessKey, @Value
            ("AWS_SECRET_ACCESS_KEY") String secretKey) {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }

    @Bean
    AmazonEC2 amazonEC2 (@Value("aws_region:us-east-2") String awsRegion, AWSStaticCredentialsProvider
            awsStaticCredentialsProvider) {
        return AmazonEC2ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
    }
}
