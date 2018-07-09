package com.gemalto.chaos;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.AttackManager;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaskSchedulerTest {
    private TaskScheduler taskScheduler;
    @Mock
    private Platform platform;
    private List<Platform> platformList;
    @Mock
    private HolidayManager holidayManager;
    @Mock
    private AttackManager attackManager;
    @Mock
    private Container container;
    private List<Container> containerList;
    @Mock
    private Attack attack;

    @Before
    public void setUp () {
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        platformList = Collections.singletonList(platform);
        containerList = Collections.singletonList(container);
    }

    @Test
    public void skipHolidays () {
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        when(holidayManager.isHoliday()).thenReturn(true);
        taskScheduler.chaosSchedule();
        verify(holidayManager, times(0)).isWorkingHours();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void skipAfterHours () {
        platformList = mock(List.class);
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(false);
        taskScheduler.chaosSchedule();
        verify(platformList, times(0)).forEach(any(Consumer.class));
    }

    @Test
    public void MultiplePlatforms () {
        platformList = Arrays.asList(platform, platform);
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(true);
        taskScheduler.chaosSchedule();
        verify(platform, times(2)).getRoster();
    }

    @Test
    public void MultipleContainers () {
        containerList = Arrays.asList(container, container, container);
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(true);
        when(platform.getRoster()).thenReturn(containerList);
        when(container.canDestroy()).thenReturn(true, false, true);
        when(container.createAttack()).thenReturn(attack, attack);
        taskScheduler.chaosSchedule();
        verify(attackManager, times(2)).addAttack(attack);
    }
}