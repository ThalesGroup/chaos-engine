package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;

import java.util.Random;
import java.util.concurrent.Callable;

public class CloudFoundryApplication extends Container {
    private String name;
    private transient CloudFoundryPlatform cloudFoundryPlatform;
    private transient Callable<Void> restageApplication = () -> {
        cloudFoundryPlatform.restageApplication(getRestageApplicationRequest());
        return null;
    };

    public CloudFoundryApplication () {
        super();
    }

    private RestageApplicationRequest getRestageApplicationRequest () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        log.info("{}", restageApplicationRequest);
        return restageApplicationRequest;
    }

    @Override
    protected Platform getPlatform () {
        return cloudFoundryPlatform;
    }

    @Override
    protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
        //TBD Check to be implemented
        return ContainerHealth.NORMAL;
    }

    @Override
    public Attack createAttack (AttackType attackType) {
        return GenericContainerAttack.builder()
                                     .withContainer(this)
                                     .withAttackType(attackType)
                                     .withTimeToLive(new Random().nextInt(5))
                                     .build();
    }

    @Override
    public String getSimpleName () {
        return name;
    }

    @StateAttack
    public Callable<Void> scaleApplication () {
        return restageApplication;
    }
}
