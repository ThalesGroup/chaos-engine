package com.gemalto.chaos.container;

import com.gemalto.chaos.attack.annotations.NetworkAttack;
import com.gemalto.chaos.attack.annotations.StateAttack;
import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class ContainerTest {
    @Mock
    private Platform platform;
    private Container testContainer = new Container() {
        private String field1 = "FIELD1";

        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
            return ContainerHealth.NORMAL;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }
    };
    private Container testContainer2 = new Container() {
        @StateAttack
        private void nullStateMethod () {
        }

        @NetworkAttack
        private void nullNetworkMethod () {
        }

        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (AttackType attackType) {
            return ContainerHealth.UNDER_ATTACK;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }
    };

    @Before
    public void setUp () {
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void canAttack () {
        // TODO : Mock the random class inside the container.
        doReturn(1D).when(platform).getDestructionProbability();
        assertFalse(testContainer.canAttack());
        doReturn(0D).when(platform).getDestructionProbability();
        assertFalse(testContainer.canAttack());

        doReturn(1D).when(platform).getDestructionProbability();
        assertTrue(testContainer2.canAttack());
        doReturn(0D).when(platform).getDestructionProbability();
        assertFalse(testContainer2.canAttack());
    }

    @Test
    public void getSupportedAttackTypes () {
        assertThat(testContainer.getSupportedAttackTypes(), IsEmptyIterable.emptyIterableOf(AttackType.class));
        assertThat(testContainer2.getSupportedAttackTypes(), IsIterableContainingInAnyOrder.containsInAnyOrder(AttackType.STATE, AttackType.NETWORK));
    }

    @Test
    public void supportsAttackType () {
        assertFalse(testContainer.supportsAttackType(AttackType.STATE));
        assertFalse(testContainer.supportsAttackType(AttackType.NETWORK));
        assertFalse(testContainer.supportsAttackType(AttackType.RESOURCE));
        assertTrue(testContainer2.supportsAttackType(AttackType.STATE));
        assertTrue(testContainer2.supportsAttackType(AttackType.NETWORK));
        assertFalse(testContainer2.supportsAttackType(AttackType.RESOURCE));
    }

    @Test
    public void getContainerHealth () {
        assertEquals(ContainerHealth.NORMAL, testContainer.getContainerHealth(null));
        assertEquals(ContainerHealth.UNDER_ATTACK, testContainer2.getContainerHealth(null));
    }
}