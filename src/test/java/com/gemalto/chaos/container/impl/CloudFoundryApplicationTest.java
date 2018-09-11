package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryApplicationTest {
    private static final String applicationId = UUID.randomUUID().toString();
    private static final int instance = new Random().nextInt(100);
    private static final String name = UUID.randomUUID().toString();
    @Spy
    private Attack attack = new Attack() {
    };
    @MockBean
    private CloudFoundryApplicationPlatform cloudFoundryApplicationPlatform;
    private CloudFoundryApplication cloudFoundryApplication;

    @Before
    public void setUp () {
        cloudFoundryApplication = CloudFoundryApplication.builder()
                                                         .applicationID(applicationId)
                                                         .containerInstances(instance)
                                                         .name(name)
                                                         .platform(cloudFoundryApplicationPlatform)
                                                         .build();
    }

    @Test
    public void scaleApplicationHealing () {
        cloudFoundryApplication.scaleApplication(attack);
        Mockito.verify(cloudFoundryApplicationPlatform, times(1)).rescaleApplication(eq(name), any(Integer.class));
        try {
            attack.getSelfHealingMethod().call();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals(cloudFoundryApplication.getOriginalContainerInstances(), cloudFoundryApplication.getActualContainerInstances());
    }

    @Test
    public void scaleApplicationFinalization () {
        cloudFoundryApplication.scaleApplication(attack);
        Mockito.verify(cloudFoundryApplicationPlatform, times(1)).rescaleApplication(eq(name), any(Integer.class));
        try {
            attack.getFinalizeMethod().call();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals(cloudFoundryApplication.getOriginalContainerInstances(), cloudFoundryApplication.getActualContainerInstances());
    }


    @Test
    public void scaleApplication () {
        cloudFoundryApplication.scaleApplication(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        verify(attack, times(1)).setFinalizeMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryApplicationPlatform, times(1)).rescaleApplication(eq(name), any(Integer.class));
    }

    @Test
    public void restageApplication () {
        RestageApplicationRequest restageApplicationRequest = RestageApplicationRequest.builder().name(name).build();
        cloudFoundryApplication.restageApplication(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryApplicationPlatform, times(1)).restageApplication(restageApplicationRequest);
    }

    @Test
    public void restartApplication () {
        cloudFoundryApplication.restartApplication(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryApplicationPlatform, times(1)).restartApplication(name);
    }

    @Test
    public void createAttack () {
        Attack attack = cloudFoundryApplication.createAttack(AttackType.RESOURCE);
        assertEquals(cloudFoundryApplication, attack.getContainer());
        assertEquals(AttackType.RESOURCE, attack.getAttackType());
    }
}