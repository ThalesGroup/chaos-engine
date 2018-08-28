package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import com.gemalto.chaos.ssh.impl.attacks.ForkBomb;
import com.gemalto.chaos.ssh.impl.attacks.RandomProcessTermination;
import org.cloudfoundry.operations.applications.RestartApplicationInstanceRequest;
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
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryContainerTest {
    private static final String applicationId = UUID.randomUUID().toString();
    private static final int instance = new Random().nextInt(100);
    private static final String name = UUID.randomUUID().toString();
    private CloudFoundryContainer cloudFoundryContainer;
    @Spy
    private Attack attack = new Attack() {
    };
    @MockBean
    private CloudFoundryContainerPlatform cloudFoundryContainerPlatform;

    @Before
    public void setUp () {
        cloudFoundryContainer = CloudFoundryContainer.builder()
                                                     .applicationId(applicationId)
                                                     .instance(instance)
                                                     .name(name)
                                                     .platform(cloudFoundryContainerPlatform)
                                                     .build();
    }

    @Test
    public void getIdentity () {
        cloudFoundryContainer = new CloudFoundryContainer("AppID", "ApplicationEngine", 1);
        CRC32 checksum = new CRC32();
        checksum.update("AppID$$$$$ApplicationEngine$$$$$1".getBytes());
        assertEquals(checksum.getValue(), cloudFoundryContainer.getIdentity());
    }

    @Test
    public void restartContainer () {
        cloudFoundryContainer.restartContainer(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .restartInstance(any(RestartApplicationInstanceRequest.class));
    }

    @Test
    public void forkBomb () {
        cloudFoundryContainer.forkBomb(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .sshAttack(any(ForkBomb.class), any(CloudFoundryContainer.class));
    }

    @Test
    public void terminateProcess () {
        cloudFoundryContainer.terminateProcess(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .sshAttack(any(RandomProcessTermination.class), any(CloudFoundryContainer.class));
    }

    @Test
    public void createAttack () {
        Attack attack = cloudFoundryContainer.createAttack(AttackType.STATE);
        assertEquals(cloudFoundryContainer, attack.getContainer());
        assertEquals(AttackType.STATE, attack.getAttackType());
    }
}