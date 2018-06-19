package com.gemalto.chaos.container.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryContainerTest {

    @Mock
    private CloudFoundryContainer cloudFoundryContainer;

    @Test
    public void getIdentity() {
        cloudFoundryContainer = new CloudFoundryContainer(
                "AppID",
                "ApplicationEngine",
                1
        );

        Checksum checksum = new CRC32();
        ((CRC32) checksum).update("AppID$$$$$ApplicationEngine$$$$$1".getBytes());


        assertEquals(checksum.getValue(), cloudFoundryContainer.getIdentity());

    }
}