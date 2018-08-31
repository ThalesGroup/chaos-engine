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
public class AwsRDSClusterContainerTest {
    private AwsRDSClusterContainer awsRDSClusterContainer;
    @MockBean
    private AwsRDSPlatform awsRDSPlatform;
    private String dbClusterIdentifier = UUID.randomUUID().toString();
    private String engine = UUID.randomUUID().toString();

    @Before
    public void setUp () {
        awsRDSClusterContainer = AwsRDSClusterContainer.builder()
                                                       .withAwsRDSPlatform(awsRDSPlatform)
                                                       .withDbClusterIdentifier(dbClusterIdentifier)
                                                       .withEngine(engine)
                                                       .build();
    }

    @Test
    public void getDbClusterIdentifier () {
        assertEquals(dbClusterIdentifier, awsRDSClusterContainer.getDbClusterIdentifier());
    }

    @Test
    public void getPlatform () {
        assertEquals(awsRDSPlatform, awsRDSClusterContainer.getPlatform());
    }

    @Test
    public void getSimpleName () {
        assertEquals(dbClusterIdentifier, awsRDSClusterContainer.getSimpleName());
    }
}