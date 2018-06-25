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

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaskSchedulerTest {
    private TaskScheduler taskScheduler;
    @Mock
    private List<Platform> platformList;
    @Mock
    private Platform platform;
    @Mock
    private Iterator<Platform> platformIterator;
    @Mock
    private HolidayManager holidayManager;
    @Mock
    private AttackManager attackManager;
    @Mock
    private List<Container> containerList;
    @Mock
    private Iterator<Container> containerIterator;
    @Mock
    private Container container;
    @Mock
    private Attack attack;

    @Before
    public void setUp () {
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
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
    public void skipAfterhours () {
        taskScheduler = new TaskScheduler(platformList, holidayManager, attackManager);
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(false);
        taskScheduler.chaosSchedule();
        verify(platformList, times(0)).forEach(any(Consumer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void MultiplePlatforms () {
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(true);
        doCallRealMethod().when(platformList).forEach(any(Consumer.class));
        when(platformList.iterator()).thenReturn(platformIterator);
        when(platformIterator.hasNext()).thenReturn(true, true, false);
        when(platformIterator.next()).thenReturn(platform, platform);
        taskScheduler.chaosSchedule();
        verify(platform, times(2)).getRoster();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void MultipleContainers () {
        when(holidayManager.isHoliday()).thenReturn(false);
        when(holidayManager.isWorkingHours()).thenReturn(true);
        doCallRealMethod().when(platformList).forEach(any(Consumer.class));
        when(platformList.iterator()).thenReturn(platformIterator);
        when(platformIterator.hasNext()).thenReturn(true, false);
        when(platformIterator.next()).thenReturn(platform);
        when(platform.getRoster()).thenReturn(containerList);
        doCallRealMethod().when(containerList).forEach(any(Consumer.class));
        when(containerList.iterator()).thenReturn(containerIterator);
        when(containerIterator.hasNext()).thenReturn(true, true, true, false);
        when(containerIterator.next()).thenReturn(container, container, container);
        when(container.canDestroy()).thenReturn(true, false, true);
        when(container.createAttack()).thenReturn(attack, attack);
        taskScheduler.chaosSchedule();
        verify(attackManager, times(2)).addAttack(attack);
    }
}