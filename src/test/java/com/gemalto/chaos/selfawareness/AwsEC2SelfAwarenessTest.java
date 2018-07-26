package com.gemalto.chaos.selfawareness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class AwsEC2SelfAwarenessTest {
    private static final String RESPONSE = UUID.randomUUID().toString();
    @Spy
    private AwsEC2SelfAwareness awsEC2SelfAwareness = new AwsEC2SelfAwareness();

    @Test
    public void isMe () {
        doReturn(RESPONSE).when(awsEC2SelfAwareness).fetchInstanceId(any(Properties.class));
        assertTrue(awsEC2SelfAwareness.isMe(RESPONSE));
    }
}