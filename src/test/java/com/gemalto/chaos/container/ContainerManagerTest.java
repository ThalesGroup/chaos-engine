package com.gemalto.chaos.container;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContainerManagerTest {
    private static final long ROSTER_KEY = 31415936L;
    private static final long SECOND_ROSTER_KEY = 985791891231L;
    @Mock
    private TestContainerClass container;
    @Mock
    private TestContainerClass2 secondContainer;
    @Mock
    private TestContainerClass2 thirdContainer;
    private HashMap<Class<? extends Container>, HashMap<Long, Container>> testContainerMap = new HashMap<>();

    @Before
    public void setUp () {
        HashMap<Long, Container> firstRoster = new HashMap<>();
        HashMap<Long, Container> secondRoster = new HashMap<>();
        testContainerMap.put(container.getClass(), firstRoster);
        testContainerMap.put(secondContainer.getClass(), secondRoster);
        firstRoster.put(ROSTER_KEY, container);
        secondRoster.put(SECOND_ROSTER_KEY, secondContainer);
    }

    @Test
    public void getRoster () {
        ContainerManager containerManager = new ContainerManager(testContainerMap);
        HashMap<Long, Container> expectedHashMap = new HashMap<>();
        expectedHashMap.put(ROSTER_KEY, container);
        HashMap<Long, Container> secondExpectedHashMap = new HashMap<>();
        secondExpectedHashMap.put(SECOND_ROSTER_KEY, secondContainer);
        Collection<Container> roster = containerManager.getRoster(container.getClass());
        Collection<Container> secondRoster = containerManager.getRoster(secondContainer.getClass());
        assertTrue(expectedHashMap.values().containsAll(roster));
        assertTrue(roster.containsAll(expectedHashMap.values()));
        assertTrue(secondExpectedHashMap.values().containsAll(secondRoster));
        assertTrue(secondRoster.containsAll(secondExpectedHashMap.values()));
    }

    @Test
    public void getOrCreatePersistentContainer () {
        ContainerManager containerManager = new ContainerManager(testContainerMap);
        when(thirdContainer.getIdentity()).thenReturn(SECOND_ROSTER_KEY);
        Assert.assertEquals(secondContainer, containerManager.getOrCreatePersistentContainer(thirdContainer));
    }

    private abstract class TestContainerClass extends Container {
    }

    private abstract class TestContainerClass2 extends Container {
    }
}