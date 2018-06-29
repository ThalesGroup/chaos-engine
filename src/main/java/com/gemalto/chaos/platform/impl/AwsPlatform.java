package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.enums.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty({ "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY" })
public class AwsPlatform implements Platform {
    private static final Logger log = LoggerFactory.getLogger(AwsPlatform.class);
    @Autowired
    private AmazonEC2 amazonEC2;

    @Autowired
    AwsPlatform () {
        log.info("AWS Platform created");
    }

    AwsPlatform (AmazonEC2 amazonEC2) {
        this.amazonEC2 = amazonEC2;
    }

    @Override
    public List<Container> getRoster () {
        boolean done = false;
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        while (!done) {
            DescribeInstancesResult describeInstancesResult = amazonEC2.describeInstances(describeInstancesRequest);
            for (Reservation reservation : describeInstancesResult.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    log.info("{}", instance);
                }
            }
            describeInstancesRequest.setNextToken(describeInstancesResult.getNextToken());
            if (describeInstancesRequest.getNextToken() == null) {
                done = true;
            }
        }
        return null;
    }

    @Override
    public ApiStatus getApiStatus () {
        return null;
    }

    public void stopInstance (String... instanceIds) {
        amazonEC2.stopInstances(new StopInstancesRequest().withForce(true).withInstanceIds(instanceIds));
    }
}
