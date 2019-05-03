package com.gemalto.chaos.selfawareness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AwsEC2SelfAwarenessTest {
    private static final String RESPONSE = UUID.randomUUID().toString();
    @Autowired
    private AwsEC2SelfAwareness awsEC2SelfAwareness = new AwsEC2SelfAwareness();

    @Test
    public void isMe () {
        doReturn(RESPONSE).when(awsEC2SelfAwareness).fetchInstanceId();
        assertTrue(awsEC2SelfAwareness.isMe(RESPONSE));
        assertTrue(awsEC2SelfAwareness.isMe(RESPONSE)); // Assertion repeated for testing caching of the ID.
        assertFalse(awsEC2SelfAwareness.isMe(RESPONSE.substring(RESPONSE.length() - 1)));
        verify(awsEC2SelfAwareness, times(1)).fetchInstanceId();
    }

    @Configuration
    static class TestConfig {
        @Bean
        AwsEC2SelfAwareness awsEC2SelfAwareness () {
            return Mockito.spy(new AwsEC2SelfAwareness());
        }
    }
}