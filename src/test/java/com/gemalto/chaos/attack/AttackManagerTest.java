package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackState;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import com.gemalto.chaos.platform.impl.CloudFoundryApplicationPlatform;
import com.gemalto.chaos.platform.impl.CloudFoundryContainerPlatform;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AttackManagerTest {
    @Autowired
    private AttackManager attackManager;
    @MockBean
    private NotificationManager notificationManager;
    @MockBean
    private PlatformManager platformManager;
    @MockBean
    private HolidayManager holidayManager;
    @Mock
    private Attack attack1;
    @Mock
    private Attack attack2;
    @Mock
    private Attack attack3;
    @Mock
    private Container container1;
    @Mock
    private Container container2;
    @Mock
    private Container container3;
    @MockBean
    private Platform platform;


    @Test
    public void startAttacks () {
        List<Container> containerList = new ArrayList<>();
        containerList.add(container1);
        containerList.add(container2);
        when(platformManager.getPlatforms()).thenReturn(Collections.singleton(platform));
        when(platform.startAttack()).thenReturn(platform);
        when(platform.generateExperimentRoster()).thenCallRealMethod();
        when(platform.getRoster()).thenReturn(containerList);
        when(platform.canAttack()).thenReturn(true);
        when(container1.canAttack()).thenReturn(true);
        when(container1.createAttack()).thenReturn(attack1);
        when(container2.canAttack()).thenReturn(false);
        attackManager.startAttacks();
        assertThat(attackManager.getNewAttackQueue(), hasItem(attack1));
        verify(container2, times(1)).canAttack();
        verify(container2, times(0)).createAttack();
    }

    //SCT-5854
    @Test
    public void avoidOverlappingAttacks () {
        CloudFoundryApplicationPlatform pcfApplicationPlatform = mock(CloudFoundryApplicationPlatform.class);
        CloudFoundryContainerPlatform pcfContainerPlatform = mock(CloudFoundryContainerPlatform.class);
        List<Container> containerListApps = new ArrayList<>();
        containerListApps.add(container1);
        containerListApps.add(container2);
        List<Container> containerListContainers = new ArrayList<>();
        containerListContainers.add(container3);
        List<Platform> platforms = new ArrayList<>();
        platforms.add(pcfApplicationPlatform);
        platforms.add(pcfContainerPlatform);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(pcfApplicationPlatform.startAttack()).thenReturn(pcfApplicationPlatform);
        when(pcfApplicationPlatform.getRoster()).thenReturn(containerListApps);
        when(pcfApplicationPlatform.canAttack()).thenReturn(true);
        when(pcfContainerPlatform.startAttack()).thenReturn(pcfContainerPlatform);
        when(pcfContainerPlatform.getRoster()).thenReturn(containerListContainers);
        when(pcfContainerPlatform.canAttack()).thenReturn(true);
        when(container1.canAttack()).thenReturn(true);
        when(container1.createAttack()).thenReturn(attack1);
        when(container1.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(container2.canAttack()).thenReturn(true);
        when(container2.createAttack()).thenReturn(attack2);
        when(container2.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(container3.canAttack()).thenReturn(true);
        when(container3.createAttack()).thenReturn(attack3);
        when(container3.getPlatform()).thenReturn(pcfContainerPlatform);
        when(attack1.startAttack(notificationManager)).thenReturn(true);
        when(attack2.startAttack(notificationManager)).thenReturn(true);
        when(attack3.startAttack(notificationManager)).thenReturn(true);
        when(attack1.getContainer()).thenReturn(container1);
        when(attack2.getContainer()).thenReturn(container2);
        when(attack3.getContainer()).thenReturn(container3);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isOutsideWorkingHours()).thenReturn(false);

        attackManager.startAttacks();
        Queue<Attack> attacks = attackManager.getNewAttackQueue();
        int scheduledAttack = attacks.size();
        attackManager.startAttacks();
        Queue<Attack> attacks2 = attackManager.getNewAttackQueue();
        // new startAttacks invocation should not add new attack until newAttackQueue is empty
        assertEquals(attacks, attacks2);
        attackManager.updateAttackStatus();
        Set<Attack> activeAttacks = attackManager.getActiveAttacks();
        int activeAttacksCount = activeAttacks.size();
        //number active attacks should be equal to number of previously scheduled attacks
        assertEquals(scheduledAttack, activeAttacksCount);
        //all active attacks should belong to same platform layer
        Platform attackPlatform = activeAttacks.iterator().next().getContainer().getPlatform();
        activeAttacks.stream().allMatch(attack -> attack.getContainer().getPlatform().equals(attackPlatform));
    }

    @Test
    public void removeFinishedAttack () {
        CloudFoundryApplicationPlatform pcfApplicationPlatform = mock(CloudFoundryApplicationPlatform.class);
        List<Container> containerListApps = new ArrayList<>();
        containerListApps.add(container1);
        containerListApps.add(container2);
        List<Platform> platforms = new ArrayList<>();
        platforms.add(pcfApplicationPlatform);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(pcfApplicationPlatform.startAttack()).thenReturn(pcfApplicationPlatform);
        when(pcfApplicationPlatform.getRoster()).thenReturn(containerListApps);
        when(pcfApplicationPlatform.canAttack()).thenReturn(true);
        when(container1.canAttack()).thenReturn(true);
        when(container1.createAttack()).thenReturn(attack1);
        when(container1.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(container2.canAttack()).thenReturn(true);
        when(container2.createAttack()).thenReturn(attack2);
        when(container2.getPlatform()).thenReturn(pcfApplicationPlatform);
        when(attack1.startAttack(notificationManager)).thenReturn(true);
        when(attack2.startAttack(notificationManager)).thenReturn(true);
        when(attack1.getContainer()).thenReturn(container1);
        when(attack2.getContainer()).thenReturn(container2);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isOutsideWorkingHours()).thenReturn(false);
        //schedule attacks
        attackManager.startAttacks();
        attackManager.updateAttackStatus();
        //check they are active
        assertTrue(attackManager.getActiveAttacks().size() == 2);
        when(attack1.getAttackState()).thenReturn(AttackState.FINISHED);
        when(attack2.getAttackState()).thenReturn(AttackState.NOT_YET_STARTED);
        //one attack to be removed
        attackManager.updateAttackStatus();
        assertTrue(attackManager.getActiveAttacks().size() == 1);
        when(attack2.getAttackState()).thenReturn(AttackState.FINISHED);
        attackManager.updateAttackStatus();
        //all attacks should be removed
        assertTrue(attackManager.getActiveAttacks().size() == 0);
    }


    @Test
    public void attackContainerId () {
        Long containerId = new Random().nextLong();
        Collection<Platform> platforms = Collections.singleton(platform);
        List<Container> roster = new ArrayList<>();
        roster.add(container1);
        roster.add(container2);
        when(platformManager.getPlatforms()).thenReturn(platforms);
        when(platform.getRoster()).thenReturn(roster);
        when(container1.getIdentity()).thenReturn(containerId);
        when(container2.getIdentity()).thenReturn(containerId + 1);
        doReturn(attack1).when(container1).createAttack();
        assertThat(attackManager.attackContainerId(containerId), IsIterableContainingInAnyOrder.containsInAnyOrder(attack1));
        verify(container1, times(1)).createAttack();
        verify(container2, times(0)).createAttack();
    }

    @Configuration
    static class AttackManagerTestConfiguration {
        @Autowired
        private NotificationManager notificationManager;
        @Autowired
        private PlatformManager platformManager;
        @Autowired
        private HolidayManager holidayManager;

        @Bean
        AttackManager attackManager () {
            return new AttackManager(notificationManager, platformManager, holidayManager);
        }
    }
}