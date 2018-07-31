package com.gemalto.chaos.container.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFoundryContainerTest {
    @MockBean
    private CloudFoundryContainer cloudFoundryContainer;

    @Test
    public void getIdentity () {
        cloudFoundryContainer = new CloudFoundryContainer("AppID", "ApplicationEngine", 1);
        CRC32 checksum = new CRC32();
        checksum.update("AppID$$$$$ApplicationEngine$$$$$1".getBytes());
        assertEquals(checksum.getValue(), cloudFoundryContainer.getIdentity());
    }
}