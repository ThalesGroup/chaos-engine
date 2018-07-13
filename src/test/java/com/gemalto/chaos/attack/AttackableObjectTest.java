package com.gemalto.chaos.attack;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class AttackableObjectTest {
    private AttackableObject attackableObject;

    @Before
    public void setUp () {
        attackableObject = new AttackableObject() {
        };
    }

    @Test
    public void canAttack () {
        assertFalse(attackableObject.canAttack());
    }
}