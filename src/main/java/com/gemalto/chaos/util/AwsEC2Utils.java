package com.gemalto.chaos.util;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;

import java.util.Optional;

public class AwsEC2Utils {
    private static final String CHAOS_CIDR_BLOCK = "192.168.1.0/24";
    private static final Tag chaosVpcTag = new Tag().withKey("ChaosEngine").withValue("true");
    private final AmazonEC2 amazonEC2;
    private Vpc chaosVpc;

    public AwsEC2Utils (AmazonEC2 amazonEC2) {
        this.amazonEC2 = amazonEC2;
    }

    public Vpc getChaosVpc () {
        return Optional.ofNullable(chaosVpc).orElseGet(this::initChaosVpc);
    }

    synchronized Vpc initChaosVpc () {
        if (chaosVpc == null) {
            chaosVpc = amazonEC2.describeVpcs()
                                .getVpcs()
                                .stream()
                                .filter(this::isChaosVpc)
                                .findFirst()
                                .orElseGet(this::createChaosVpc);
        }
        return chaosVpc;
    }

    private boolean isChaosVpc (Vpc vpc) {
        return vpc.getTags().stream().anyMatch(chaosVpcTag::equals);
    }

    private Vpc createChaosVpc () {
        Vpc createdVPC = amazonEC2.createVpc(new CreateVpcRequest().withCidrBlock(CHAOS_CIDR_BLOCK)).getVpc();
        amazonEC2.createTags(new CreateTagsRequest().withTags(chaosVpcTag).withResources(createdVPC.getVpcId()));
        return createdVPC;
    }
}
