package com.gemalto.chaos.selfawareness;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudFoundrySelfAwarenessTest {
    private static final String engineName = "THIS IS MY ENGINE";
    private static final Integer engineId = 271828;
    private static final String[] linkedApplications = { "ChaosMongo", "ChaosUtil" };
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    @Before
    public void setUp () {
        cloudFoundrySelfAwareness = new CloudFoundrySelfAwareness(engineName, engineId, linkedApplications);
    }

    @Test
    public void isMe () {
        assertTrue(cloudFoundrySelfAwareness.isMe(engineName, engineId));
        assertFalse(cloudFoundrySelfAwareness.isMe(engineName, 1));
        assertFalse(cloudFoundrySelfAwareness.isMe("Bob", engineId));
        assertFalse(cloudFoundrySelfAwareness.isMe("Bob", 1));
    }

    @Test
    public void isLinkedApplication () {
        assertTrue(cloudFoundrySelfAwareness.isFriendly("ChaosMongo"));
        assertTrue(cloudFoundrySelfAwareness.isFriendly("ChaosUtil"));
        assertFalse(cloudFoundrySelfAwareness.isFriendly(engineName));
    }
}