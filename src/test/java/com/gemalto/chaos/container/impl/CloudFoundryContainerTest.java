package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import com.gemalto.chaos.ssh.ShellSessionCapability;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
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

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    public void restartContainer () throws Exception {
        cloudFoundryContainer.restartContainer(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .restartInstance(any(RestartApplicationInstanceRequest.class));
        attack.getSelfHealingMethod().call();
    }

    @Test
    public void forkBomb () throws Exception {
        cloudFoundryContainer.forkBomb(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .sshAttack(any(ForkBomb.class), any(CloudFoundryContainer.class));
        attack.getSelfHealingMethod().call();
    }

    @Test
    public void terminateProcess () throws Exception {
        cloudFoundryContainer.terminateProcess(attack);
        verify(attack, times(1)).setCheckContainerHealth(ArgumentMatchers.any());
        verify(attack, times(1)).setSelfHealingMethod(ArgumentMatchers.any());
        Mockito.verify(cloudFoundryContainerPlatform, times(1))
               .sshAttack(any(RandomProcessTermination.class), any(CloudFoundryContainer.class));
        attack.getSelfHealingMethod().call();
    }

    @Test
    public void createAttack () {
        Attack attack = cloudFoundryContainer.createAttack(AttackType.STATE);
        assertEquals(cloudFoundryContainer, attack.getContainer());
        assertEquals(AttackType.STATE, attack.getAttackType());
    }

    @Test
    public void getSimpleName () {
        String EXPECTED_NAME = String.format("%s - (%s)", cloudFoundryContainer.getName(), cloudFoundryContainer.getInstance());
        assertEquals(EXPECTED_NAME, cloudFoundryContainer.getSimpleName());
    }

    @Test
    public void detectedCapabilities () {
        ShellSessionCapability capabilityBash = new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH);
        ShellSessionCapability capabilityBinary = new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.GREP)
                                                                                                        .addCapabilityOption(ShellSessionCapabilityOption.SORT);
        ArrayList<ShellSessionCapability> capabilities = new ArrayList<>();
        capabilities.add(capabilityBash);
        capabilities.add(capabilityBinary);
        cloudFoundryContainer.setDetectedCapabilities(capabilities);
        assertEquals(capabilities, cloudFoundryContainer.getDetectedCapabilities());
    }

    @Test
    public void getApplicationID () {
        assertEquals(applicationId, cloudFoundryContainer.getApplicationId());
    }

    @Test
    public void getPlatform () {
        assertEquals(cloudFoundryContainerPlatform, cloudFoundryContainer.getPlatform());
    }

    @Test
    public void updateContainerHealthImpl () {
        doReturn(ContainerHealth.NORMAL).when(cloudFoundryContainerPlatform).checkHealth(applicationId, instance);
        assertEquals(ContainerHealth.NORMAL, cloudFoundryContainer.updateContainerHealthImpl(AttackType.STATE));
    }
}