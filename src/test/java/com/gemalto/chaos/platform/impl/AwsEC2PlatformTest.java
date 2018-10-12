package com.gemalto.chaos.platform.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.gemalto.chaos.constants.AwsEC2Constants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.ContainerManager;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import com.gemalto.chaos.selfawareness.AwsEC2SelfAwareness;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.gemalto.chaos.constants.AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class AwsEC2PlatformTest {
    @MockBean
    private AmazonEC2 amazonEC2;
    @Mock
    private DescribeInstancesResult describeInstancesResult;
    @Mock
    private Reservation reservation;
    @Mock
    private Instance instance;
    @MockBean
    private ContainerManager containerManager;
    @MockBean
    private AwsEC2SelfAwareness awsEC2SelfAwareness;
    @Autowired
    private AwsEC2Platform awsEC2Platform;

    @Test
    public void getRoster () {
        final Instance instance2 = mock(Instance.class);
        final String INSTANCE_NAME_1 = "NamedDevice";
        final String INSTANCE_KEYNAME_1 = randomUUID().toString();
        final String INSTANCE_ID_1 = randomUUID().toString();
        final String INSTANCE_KEYNAME_2 = randomUUID().toString();
        final String INSTANCE_ID_2 = randomUUID().toString();
        final List<Reservation> reservationList = Collections.singletonList(reservation);
        final List<Instance> instanceList = Arrays.asList(instance, instance2);
        final Tag namedTag = new Tag("Name", INSTANCE_NAME_1);
        final InstanceState instanceState = new InstanceState().withCode(AwsEC2Constants.AWS_RUNNING_CODE);
        final AwsEC2Container CONTAINER_1 = AwsEC2Container.builder()
                                                           .instanceId(INSTANCE_ID_1)
                                                           .keyName(INSTANCE_KEYNAME_1)
                                                           .name(INSTANCE_NAME_1)
                                                           .build();
        final AwsEC2Container CONTAINER_2 = AwsEC2Container.builder()
                                                           .instanceId(INSTANCE_ID_2)
                                                           .keyName(INSTANCE_KEYNAME_2)
                                                           .name("no-name")
                                                           .build();
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(reservationList);
        when(reservation.getInstances()).thenReturn(instanceList);
        when(instance.getTags()).thenReturn(Collections.singletonList(namedTag));
        when(instance2.getTags()).thenReturn(Collections.emptyList());
        when(describeInstancesResult.getNextToken()).thenReturn(null);
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID_1);
        when(instance.getKeyName()).thenReturn(INSTANCE_KEYNAME_1);
        when(instance.getState()).thenReturn(instanceState);
        when(instance2.getInstanceId()).thenReturn(INSTANCE_ID_2);
        when(instance2.getKeyName()).thenReturn(INSTANCE_KEYNAME_2);
        when(instance2.getState()).thenReturn(instanceState);
        when(containerManager.getOrCreatePersistentContainer(any(Container.class))).thenAnswer((Answer<Container>) invocation -> (Container) invocation
                .getArguments()[0]);
        final List<Container> roster = awsEC2Platform.getRoster();
        assertThat(roster, IsIterableContainingInAnyOrder.containsInAnyOrder(CONTAINER_1, CONTAINER_2));
    }

    @Test
    public void getApiStatusSuccess () {
        when(amazonEC2.describeInstances()).thenReturn(null);
        assertEquals(ApiStatus.OK, awsEC2Platform.getApiStatus());
    }

    @Test
    public void getApiStatusFail () {
        when(amazonEC2.describeInstances()).thenThrow(mock(RuntimeException.class));
        assertEquals(ApiStatus.ERROR, awsEC2Platform.getApiStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkHealth () {
        List<Instance> instanceList = Collections.singletonList(instance);
        List<Reservation> reservationList = Collections.singletonList(reservation);
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(reservationList);
        when(reservation.getInstances()).thenReturn(instanceList);
        when(instance.getState()).thenReturn(new InstanceState().withCode(16));
        assertEquals(ContainerHealth.NORMAL, awsEC2Platform.checkHealth(null));
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(reservationList);
        when(reservation.getInstances()).thenReturn(instanceList);
        when(instance.getState()).thenReturn(new InstanceState().withCode(0));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Platform.checkHealth(null));
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(Collections.EMPTY_LIST);
        assertEquals(ContainerHealth.DOES_NOT_EXIST, awsEC2Platform.checkHealth(null));
    }

    @Test
    public void stopInstance () {
        awsEC2Platform.stopInstance("123", "abc", "xyz");
        verify(amazonEC2, times(1)).stopInstances(any(StopInstancesRequest.class));
    }

    @Test
    public void terminateInstance () {
        awsEC2Platform.terminateInstance("123", "abc", "xyz");
        verify(amazonEC2, times(1)).terminateInstances(any(TerminateInstancesRequest.class));
    }

    @Test
    public void startInstance () {
        awsEC2Platform.startInstance("123", "abc", "xyz");
        verify(amazonEC2, times(1)).startInstances(any(StartInstancesRequest.class));
    }

    @Test
    public void getPlatformLevel () {
        assertEquals(PlatformLevel.IAAS, awsEC2Platform.getPlatformLevel());
    }

    @Test
    public void getPlatformHealth () {
        List<Instance> instanceList = Collections.singletonList(instance);
        List<Reservation> reservationList = Collections.singletonList(reservation);
        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(reservationList);
        when(reservation.getInstances()).thenReturn(instanceList);
        when(instance.getState()).thenReturn(new InstanceState().withCode(0));
        assertEquals(PlatformHealth.DEGRADED, awsEC2Platform.getPlatformHealth());
        when(instance.getState()).thenReturn(new InstanceState().withCode(16));
        assertEquals(PlatformHealth.OK, awsEC2Platform.getPlatformHealth());
    }

    @Test
    public void restartInstance () {
        awsEC2Platform.restartInstance("123", "abc", "xyz");
        verify(amazonEC2, times(1)).rebootInstances(any(RebootInstancesRequest.class));
    }

    @Test
    public void getSecurityGroupIds () {
        String groupId = UUID.randomUUID().toString();
        GroupIdentifier groupIdentifier = new GroupIdentifier().withGroupName("Test Group").withGroupId(groupId);
        DescribeInstanceAttributeResult result = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute()
                .withGroups(groupIdentifier));
        doReturn(result).when(amazonEC2).describeInstanceAttribute(any(DescribeInstanceAttributeRequest.class));
        assertEquals(Collections.singletonList(groupId), awsEC2Platform.getSecurityGroupIds("123"));
    }

    @Test
    public void setSecurityGroupIds () {
        String instanceId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        awsEC2Platform.setSecurityGroupIds(instanceId, Collections.singletonList(groupId));
        verify(amazonEC2, times(1)).modifyInstanceAttribute(any());
    }

    @Test
    public void getChaosSecurityGroupId () {
        Vpc defaultVpc = new Vpc().withIsDefault(true).withVpcId(UUID.randomUUID().toString());
        Vpc customVpc = new Vpc().withIsDefault(false).withVpcId(UUID.randomUUID().toString());
        SecurityGroup defaultSecurityGroup = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        SecurityGroup customSecurityGroup1 = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        SecurityGroup customSecurityGroup2 = new SecurityGroup().withVpcId(customVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        DescribeSecurityGroupsResult securityGroupsResult = new DescribeSecurityGroupsResult().withSecurityGroups(defaultSecurityGroup, customSecurityGroup1, customSecurityGroup2);
        DescribeVpcsResult vpcsResult = new DescribeVpcsResult().withVpcs(defaultVpc, customVpc);
        doReturn(securityGroupsResult).when(amazonEC2).describeSecurityGroups();
        doReturn(vpcsResult).when(amazonEC2).describeVpcs();
        SecurityGroup chaosSecurityGroup = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                              .withGroupName(EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME)
                                                              .withGroupId(UUID.randomUUID().toString());
        CreateSecurityGroupResult createSecurityGroupResult = new CreateSecurityGroupResult().withGroupId(chaosSecurityGroup
                .getGroupId());
        doReturn(createSecurityGroupResult).when(amazonEC2).createSecurityGroup(any());
        assertEquals(chaosSecurityGroup.getGroupId(), awsEC2Platform.getChaosSecurityGroupId());
        verify(amazonEC2, times(1)).describeVpcs();
        verify(amazonEC2, times(1)).createSecurityGroup(any());
        verify(awsEC2Platform, times(1)).initChaosSecurityGroupId();
        awsEC2Platform.getChaosSecurityGroupId();
        verify(awsEC2Platform, times(1)).initChaosSecurityGroupId();
    }

    @Test
    public void getChaosSecurityGroupIdAlreadyInitialized () {
        Vpc defaultVpc = new Vpc().withIsDefault(true).withVpcId(UUID.randomUUID().toString());
        Vpc customVpc = new Vpc().withIsDefault(false).withVpcId(UUID.randomUUID().toString());
        SecurityGroup defaultSecurityGroup = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        SecurityGroup customSecurityGroup1 = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        SecurityGroup customSecurityGroup2 = new SecurityGroup().withVpcId(customVpc.getVpcId())
                                                                .withGroupName(UUID.randomUUID().toString());
        SecurityGroup chaosSecurityGroup = new SecurityGroup().withVpcId(defaultVpc.getVpcId())
                                                              .withGroupName(EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME)
                                                              .withGroupId(UUID.randomUUID().toString());
        DescribeSecurityGroupsResult securityGroupsResult = new DescribeSecurityGroupsResult().withSecurityGroups(defaultSecurityGroup, chaosSecurityGroup, customSecurityGroup1, customSecurityGroup2);
        DescribeVpcsResult vpcsResult = new DescribeVpcsResult().withVpcs(defaultVpc, customVpc);
        doReturn(securityGroupsResult).when(amazonEC2).describeSecurityGroups();
        doReturn(vpcsResult).when(amazonEC2).describeVpcs();
        assertEquals(chaosSecurityGroup.getGroupId(), awsEC2Platform.getChaosSecurityGroupId());
        verify(amazonEC2, times(1)).describeVpcs();
        verify(amazonEC2, times(0)).createSecurityGroup(any());
    }

    @Test
    public void verifySecurityGroupIds () {
        String instanceId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String groupId2 = UUID.randomUUID().toString();
        GroupIdentifier groupIdentifier = new GroupIdentifier().withGroupName("Test Group").withGroupId(groupId);
        GroupIdentifier groupIdentifier2 = new GroupIdentifier().withGroupName("Test Group 2").withGroupId(groupId2);
        DescribeInstanceAttributeResult result = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute()
                .withGroups(groupIdentifier));
        doReturn(result).when(amazonEC2).describeInstanceAttribute(any(DescribeInstanceAttributeRequest.class));
        assertEquals(ContainerHealth.NORMAL, awsEC2Platform.verifySecurityGroupIds(instanceId, Collections.singletonList(groupId)));
        doReturn(new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withGroups(groupIdentifier, groupIdentifier2)))
                .when(amazonEC2)
                .describeInstanceAttribute(any());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Platform.verifySecurityGroupIds(instanceId, Collections.singletonList(groupId)));
        doReturn(new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withGroups(groupIdentifier)))
                .when(amazonEC2)
                .describeInstanceAttribute(any());
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Platform.verifySecurityGroupIds(instanceId, Arrays.asList(groupId, groupId2)));
    }

    @Configuration
    static class AwsEC2PlatformTestConfiguration {
        @Autowired
        private AmazonEC2 amazonEC2;
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private AwsEC2SelfAwareness awsEC2SelfAwareness;

        @Bean
        AwsEC2Platform awsEC2Platform () {
            return Mockito.spy(new AwsEC2Platform(null, null, amazonEC2, containerManager, awsEC2SelfAwareness));
        }
    }
}