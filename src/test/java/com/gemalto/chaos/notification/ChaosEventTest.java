package com.gemalto.chaos.notification;

import com.gemalto.chaos.container.Container;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class ChaosEventTest {
    private static final String chaosMessage = "It's chaos time!";
    @Mock
    private Container container;
    @Mock
    private Date date;

    @Test
    public void testToString () {
        ChaosEvent chaosEvent = ChaosEvent.builder()
                                          .withChaosTime(date)
                                          .withMessage(chaosMessage)
                                          .withTargetContainer(container)
                                          .build();
        Mockito.when(container.toString()).thenReturn("ChaosEventTestContainer");
        Mockito.when(date.toString()).thenReturn("Chaos-O'Clock");
        String expectedString = "Chaos Event: [targetContainer=ChaosEventTestContainer]" + "[chaosTime=Chaos-O'Clock]" + "[message=It's chaos time!]";
        Assert.assertEquals(expectedString, chaosEvent.toString());
    }
}