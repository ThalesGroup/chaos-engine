package com.gemalto.chaos.attack;

import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class AttackManagerTest {
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
    private Container container1;
    @Mock
    private Container container2;
    @MockBean
    private Platform platform;

    @Before
    public void setUp () {
        attackManager = new AttackManager(notificationManager, platformManager, holidayManager);
    }

    @Test
    public void startAttacks () {
        List<Container> containerList = new ArrayList<>();
        containerList.add(container1);
        containerList.add(container2);
        when(platformManager.getPlatforms()).thenReturn(Collections.singleton(platform));
        when(platform.startAttack()).thenReturn(platform);
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
}