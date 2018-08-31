package com.gemalto.chaos.container.impl;

import com.gemalto.chaos.platform.impl.AwsRDSPlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class AwsRDSInstanceContainerTest {
    @MockBean
    private AwsRDSPlatform awsRDSPlatform;
    private AwsRDSInstanceContainer awsRDSInstanceContainer;
    private String dbInstanceIdentifier = UUID.randomUUID().toString();
    private String engine = UUID.randomUUID().toString();

    @Before
    public void setUp () {
        awsRDSInstanceContainer = AwsRDSInstanceContainer.builder()
                                                         .withEngine(engine)
                                                         .withDbInstanceIdentifier(dbInstanceIdentifier)
                                                         .withAwsRDSPlatform(awsRDSPlatform)
                                                         .build();
    }

    @Test
    public void getDbInstanceIdentifier () {
        assertEquals(dbInstanceIdentifier, awsRDSInstanceContainer.getDbInstanceIdentifier());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsRDSPlatform, awsRDSInstanceContainer.getPlatform());
    }

    @Test
    public void getSimpleName () {
        assertEquals(dbInstanceIdentifier, awsRDSInstanceContainer.getSimpleName());
    }
}