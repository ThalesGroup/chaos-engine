package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlatformTest {
    @Mock
    private Container container;
    @Spy
    private Platform platform;

    @Before
    public void setUp () {
        platform = Mockito.spy(new Platform() {
            @Override
            public List<Container> generateRoster () {
                return Collections.singletonList(container);
            }

            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void getSupportedAttackTypes () {
        doReturn(Collections.singletonList(AttackType.STATE)).when(container).getSupportedAttackTypes();
        assertThat(platform.getSupportedAttackTypes(), IsIterableContainingInAnyOrder.containsInAnyOrder(AttackType.STATE));
        assertThat(platform.getSupportedAttackTypes(), IsIterableContainingInAnyOrder.containsInAnyOrder(AttackType.STATE));
        Mockito.verify(container, times(1)).getSupportedAttackTypes();
    }

    @Test
    public void getRoster () {
        platform.getRoster();
        platform.getRoster();
        verify(platform, times(1)).generateRoster();
        assertThat(platform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(container));
    }

    @Test
    public void expireCachedRoster () {
        platform.getRoster();
        platform.expireCachedRoster();
        platform.getRoster();
        verify(platform, times(2)).generateRoster();
        assertThat(platform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(container));
    }

    @Test
    public void calculateMTBFPercentile () {
        assertEquals(0.5F, Platform.calculateMTBFPercentile(1, 1), 0.0001);
        assertEquals(0.292893F, Platform.calculateMTBFPercentile(1, 2), 0.000001);
        assertEquals(0.646447F, Platform.calculateMTBFPercentile(3, 2), 0.000001);
        assertEquals(0.206299F, Platform.calculateMTBFPercentile(1, 3), 0.000001);
        assertEquals(0.370039F, Platform.calculateMTBFPercentile(2, 3), 0.000001);
        assertEquals(0.603150F, Platform.calculateMTBFPercentile(4, 3), 0.000001);
        assertEquals(0.159104F, Platform.calculateMTBFPercentile(1, 4), 0.000001);
    }
}