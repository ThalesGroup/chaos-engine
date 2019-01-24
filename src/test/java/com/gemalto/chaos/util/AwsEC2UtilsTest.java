package com.gemalto.chaos.util;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AwsEC2UtilsTest {
    private final Vpc otherVpc = new Vpc().withVpcId(UUID.randomUUID().toString())
                                          .withIsDefault(false)
                                          .withCidrBlock("10.11.12.0/24")
                                          .withState(VpcState.Available)
                                          .withTags(new Tag("Product", "WebApp"));
    private final Vpc chaosVpc = new Vpc().withVpcId(UUID.randomUUID().toString())
                                          .withIsDefault(false)
                                          .withCidrBlock("192.168.1.0/24")
                                          .withState(VpcState.Available)
                                          .withTags(new Tag("ChaosEngine", "true"));
    private final DescribeVpcsResult describeOnlyOtherVpc = new DescribeVpcsResult().withVpcs(otherVpc);
    private final DescribeVpcsResult describeBothVpcs = new DescribeVpcsResult().withVpcs(otherVpc, chaosVpc);
    @MockBean
    private AmazonEC2 amazonEC2;
    @Autowired
    private AwsEC2Utils awsEC2Utils;

    @Test
    public void getChaosVpcNotCreated () {
        doReturn(describeOnlyOtherVpc).when(amazonEC2).describeVpcs();
        doReturn(new CreateVpcResult().withVpc(chaosVpc)).when(amazonEC2).createVpc(any());
        doReturn(null).when(amazonEC2).createTags(any());
        assertEquals(chaosVpc, awsEC2Utils.getChaosVpc());
        verify(amazonEC2, times(1)).describeVpcs();
        verify(amazonEC2, times(1)).createVpc(any());
        verify(amazonEC2, times(1)).createTags(any());
        reset(amazonEC2);
        assertEquals(chaosVpc, awsEC2Utils.getChaosVpc());
        verify(amazonEC2, never()).describeVpcs();
    }

    @Test
    public void getChaosVpcNotCached () {
        doReturn(describeBothVpcs).when(amazonEC2).describeVpcs();
        assertEquals(chaosVpc, awsEC2Utils.getChaosVpc());
        verify(amazonEC2, times(1)).describeVpcs();
        verify(amazonEC2, never()).createVpc(any());
        verify(amazonEC2, never()).createTags(any());
        reset(amazonEC2);
        assertEquals(chaosVpc, awsEC2Utils.getChaosVpc());
        verify(amazonEC2, never()).describeVpcs();
    }

    @Test
    public void getChaosVpcThreadsafe () throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(2);
        doAnswer((Answer<DescribeVpcsResult>) invocationOnMock -> {
            Thread.sleep(500);
            return describeBothVpcs;
        }).when(amazonEC2).describeVpcs();
        Future<?> firstCall = service.submit(() -> awsEC2Utils.getChaosVpc());
        Future<?> secondCall = service.submit(() -> awsEC2Utils.getChaosVpc());
        Awaitility.await()
                  .atLeast(Duration.ONE_HUNDRED_MILLISECONDS)
                  .atMost(Duration.ONE_SECOND)
                  .until(firstCall::isDone);
        Awaitility.await()
                  .atLeast(Duration.ONE_HUNDRED_MILLISECONDS)
                  .atMost(Duration.ONE_SECOND)
                  .until(secondCall::isDone);
        assertEquals(chaosVpc, firstCall.get());
        assertEquals(chaosVpc, secondCall.get());
        verify(amazonEC2, atMost(1)).describeVpcs();
        verify(awsEC2Utils, times(2)).initChaosVpc();
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private AmazonEC2 amazonEC2;

        @Bean
        AwsEC2Utils awsEC2Utils () {
            return spy(new AwsEC2Utils(amazonEC2));
        }
    }
}