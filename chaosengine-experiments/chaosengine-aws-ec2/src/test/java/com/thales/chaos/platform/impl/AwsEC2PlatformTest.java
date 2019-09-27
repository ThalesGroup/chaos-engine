/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.platform.impl;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.SetInstanceHealthRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.thales.chaos.constants.AwsEC2Constants;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.container.impl.AwsEC2Container;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.selfawareness.AwsEC2SelfAwareness;
import org.apache.commons.net.util.SubnetUtils;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;
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
    @SpyBean
    private ContainerManager containerManager;
    @MockBean
    private AwsEC2SelfAwareness awsEC2SelfAwareness;
    @MockBean
    private AmazonAutoScaling amazonAutoScaling;
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
        final List<Instance> instanceList = asList(instance, instance2);
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
        doReturn(new Placement("us-east-1b")).when(instance).getPlacement();
        doReturn(new Placement("us-east-1c")).when(instance2).getPlacement();
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
        final List<Container> roster = awsEC2Platform.getRoster();
        assertThat(roster, containsInAnyOrder(CONTAINER_1, CONTAINER_2));
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
        verify(amazonEC2, times(1)).modifyNetworkInterfaceAttribute(any());
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
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, awsEC2Platform.verifySecurityGroupIds(instanceId, asList(groupId, groupId2)));
    }

    @Test
    public void createContainerFromInstance () {
        String instanceId = UUID.randomUUID().toString();
        // Should return null for a terminated instance
        Instance instance = Mockito.spy(new Instance().withState(new InstanceState().withCode(AwsEC2Constants.AWS_TERMINATED_CODE)
                                                                                    .withName(InstanceStateName.Terminated)));
        assertNull(awsEC2Platform.createContainerFromInstance(instance));
        verify(containerManager, times(0)).getMatchingContainer(any(), any());
        reset(awsEC2Platform, containerManager);
        // Container that hasn't been created before
        instance = Mockito.spy(new Instance().withState(new InstanceState().withCode(0).withName(InstanceStateName.Running))
                                             .withInstanceId(instanceId));
        AwsEC2Container container = Mockito.spy(AwsEC2Container.builder().instanceId(instanceId).build());
        doReturn(container).when(awsEC2Platform).buildContainerFromInstance(instance);
        doCallRealMethod().when(awsEC2Platform).createContainerFromInstance(instance);
        assertSame(container, awsEC2Platform.createContainerFromInstance(instance));
        verify(containerManager, times(1)).offer(container);
        // Container HAS been created before
        reset(awsEC2Platform, containerManager);
        doCallRealMethod().when(awsEC2Platform).createContainerFromInstance(instance);
        assertSame(container, awsEC2Platform.createContainerFromInstance(instance));
        verify(awsEC2Platform, times(0)).buildContainerFromInstance(instance);
        verify(containerManager, times(0)).offer(container);
    }

    @Test
    public void buildContainerFromInstance () {
        String instanceId = UUID.randomUUID().toString();
        String name = UUID.randomUUID().toString();
        String keyName = UUID.randomUUID().toString();
        String availabilityZone = UUID.randomUUID().toString();
        Placement placement = new Placement().withAvailabilityZone(availabilityZone);
        Instance instance;
        AwsEC2Container container;
        // With Name
        instance = new Instance().withInstanceId(instanceId).withTags(new Tag("Name", name)).withKeyName(keyName).withPlacement(placement);
        container = AwsEC2Container.builder().instanceId(instanceId).keyName(keyName).name(name).build();
        assertEquals(container, awsEC2Platform.buildContainerFromInstance(instance));
        // With no name
        instance = new Instance().withInstanceId(instanceId).withKeyName(keyName);
        container = AwsEC2Container.builder().instanceId(instanceId).name("no-name").keyName(keyName).build();
        assertEquals(container, awsEC2Platform.buildContainerFromInstance(instance));
        // With Grouping Tag
        ReflectionTestUtils.setField(awsEC2Platform, "groupingTags", asList("asg", "bosh", "somethingelse"));
        instance = new Instance().withInstanceId(instanceId).withKeyName(keyName).withTags(new Tag("Name", name), new Tag("asg", "scalegroup"));
        container = AwsEC2Container.builder()
                                   .instanceId(instanceId)
                                   .keyName(keyName)
                                   .name(name)
                                   .groupIdentifier("scalegroup")
                                   .build();
        assertEquals(container, awsEC2Platform.buildContainerFromInstance(instance));
        // With multiple grouping tags
        instance = new Instance().withInstanceId(instanceId).withKeyName(keyName).withTags(new Tag("Name", name), new Tag("bosh", "boshgroup"), new Tag("asg", "scalegroup"));
        container = AwsEC2Container.builder()
                                   .instanceId(instanceId)
                                   .keyName(keyName)
                                   .name(name)
                                   .groupIdentifier("scalegroup")
                                   .build();
        assertEquals(container, awsEC2Platform.buildContainerFromInstance(instance));
    }

    @Test
    public void generateExperimentRoster () {
        List<String> groupIdentifiers = IntStream.range(0, 4).mapToObj(i -> randomUUID().toString()).collect(Collectors.toList());
        List<Container> containers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            containers.add(AwsEC2Container.builder()
                                          .groupIdentifier(groupIdentifiers.get(i % groupIdentifiers.size()))
                                          .name(randomUUID().toString())
                                          .instanceId(randomUUID().toString())
                                          .build());
        }
        // Container without a group
        AwsEC2Container independentContainer = AwsEC2Container.builder()
                                                              .name(randomUUID().toString())
                                                              .instanceId(randomUUID().toString())
                                                              .build();
        containers.add(independentContainer);
        // Container with no other group members.
        AwsEC2Container lonelyScaledContainer = AwsEC2Container.builder()
                                                               .name(randomUUID().toString())
                                                               .instanceId(randomUUID().toString())
                                                               .groupIdentifier(randomUUID().toString())
                                                               .build();
        containers.add(lonelyScaledContainer);
        doReturn(containers).when(awsEC2Platform).getRoster();
        Map<String, List<AwsEC2Container>> groupedContainers = containers.stream().map(AwsEC2Container.class::cast).collect(Collectors.groupingBy(AwsEC2Container::getGroupIdentifier));
        List<Container> experimentRoster = awsEC2Platform.generateExperimentRoster();
        assertThat(experimentRoster, IsIterableWithSize.iterableWithSize(8));
        assertTrue("Should always contain container that is not grouped.", experimentRoster.contains(independentContainer));
        assertTrue("Should always contain container that has no others in group.", experimentRoster.contains(lonelyScaledContainer));
    }

    @Test
    public void triggerAutoscalingUnhealthy () {
        String instanceId = randomUUID().toString();
        awsEC2Platform.triggerAutoscalingUnhealthy(instanceId);
        verify(amazonAutoScaling, atLeastOnce()).setInstanceHealth(new SetInstanceHealthRequest().withHealthStatus("Unhealthy")
                                                                                                 .withInstanceId(instanceId)
                                                                                                 .withShouldRespectGracePeriod(false));
    }

    @Test
    public void isContainerTerminatedTrue () {
        String instanceId = randomUUID().toString();
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(new Reservation()
                .withInstances(new Instance().withInstanceId(instanceId).withState(new InstanceState().withCode(AwsEC2Constants.AWS_TERMINATED_CODE))));
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertTrue("One instance returned in terminated state should return true", awsEC2Platform.isContainerTerminated(instanceId));
        describeInstancesResult = new DescribeInstancesResult().withReservations(new Reservation().withInstances(new Instance()
                .withInstanceId(instanceId)
                .withState(new InstanceState().withCode(AwsEC2Constants.AWS_RUNNING_CODE))));
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertFalse("One instance returned in running state should return false", awsEC2Platform.isContainerTerminated(instanceId));
    }

    @Test
    public void isAutoscalingGroupAtDesiredInstances () {
        String groupId = randomUUID().toString();
        final int DESIRED_CAPACITY = new Random().nextInt(10) + 5;
        List<com.amazonaws.services.autoscaling.model.Instance> instances = IntStream.range(0, DESIRED_CAPACITY)
                                                                                     .mapToObj(i -> new com.amazonaws.services.autoscaling.model.Instance()
                                                                                             .withHealthStatus("Healthy"))
                                                                                     .collect(Collectors.toList());
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withInstances(instances).withDesiredCapacity(DESIRED_CAPACITY);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult().withAutoScalingGroups(Collections
                .singletonList(autoScalingGroup));
        doReturn(describeAutoScalingGroupsResult).when(amazonAutoScaling).describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupId));
        assertTrue("An equal size set of healthy instances should be true", awsEC2Platform.isAutoscalingGroupAtDesiredInstances(groupId));
        autoScalingGroup.getInstances().get(0).setHealthStatus("Unhealthy");
        assertFalse("An unhealthy instance should not be counted", awsEC2Platform.isAutoscalingGroupAtDesiredInstances(groupId));
        autoScalingGroup.getInstances().remove(0);
        autoScalingGroup.getInstances().forEach(instance1 -> instance1.setHealthStatus("Healthy"));
        assertFalse("Insufficient total instance should be false", awsEC2Platform.isAutoscalingGroupAtDesiredInstances(groupId));
    }

    @Test
    public void generateSearchFilters () {
        Map<String, List<String>> filterConfig = new HashMap<>();
        filterConfig.put("tag.Filter1", Collections.singletonList("Value1"));
        filterConfig.put("tag.Filter2", asList("Value2a", "Value2b"));
        filterConfig.put("keyName", Collections.singletonList("keyValue1"));
        filterConfig.put("KeyName", Collections.singletonList("keyValue2"));
        Filter filter1 = new Filter("tag:Filter1", Collections.singletonList("Value1"));
        Filter filter2 = new Filter("tag:Filter2", asList("Value2a", "Value2b"));
        Filter filter3 = new Filter("key-name", Collections.singletonList("keyValue1"));
        Filter filter4 = new Filter("key-name", Collections.singletonList("keyValue2"));
        awsEC2Platform.setFilter(filterConfig);
        Collection<Filter> filters = awsEC2Platform.generateSearchFilters();
        assertThat(filters, containsInAnyOrder(filter1, filter2, filter3, filter4));
    }

    @Test
    public void getVpcIdOfContainer () {
        String instanceId = randomUUID().toString();
        String vpcId = randomUUID().toString();
        Instance instance = new Instance().withInstanceId(instanceId).withVpcId(vpcId);
        Reservation reservation = new Reservation().withInstances(instance);
        ArgumentCaptor<DescribeInstancesRequest> captor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        doReturn(new DescribeInstancesResult().withReservations(reservation)).when(amazonEC2).describeInstances(captor.capture());
        assertEquals(vpcId, awsEC2Platform.getVpcIdOfContainer(instanceId));
        assertEquals(Collections.singletonList(instanceId), captor.getValue().getInstanceIds());
    }

    @Test(expected = ChaosException.class)
    public void getVpcIdOfContainerException () {
        doReturn(new DescribeInstancesResult().withReservations(new Reservation())).when(amazonEC2).describeInstances(any());
        awsEC2Platform.getVpcIdOfContainer(randomUUID().toString());
    }

    @Test
    public void getChaosSecurityGroupIdOfInstance () {
        String instanceId = randomUUID().toString();
        String vpcId = randomUUID().toString();
        String securityGroupId = randomUUID().toString();
        doReturn(vpcId).when(awsEC2Platform).getVpcIdOfContainer(instanceId);
        doReturn(securityGroupId).when(awsEC2Platform).lookupChaosSecurityGroup(vpcId);
        assertEquals(securityGroupId, awsEC2Platform.getChaosSecurityGroupForInstance(instanceId));
        verify(awsEC2Platform, times(1)).lookupChaosSecurityGroup(vpcId);
        reset(awsEC2Platform);
        doReturn(vpcId).when(awsEC2Platform).getVpcIdOfContainer(instanceId);
        assertEquals(securityGroupId, awsEC2Platform.getChaosSecurityGroupForInstance(instanceId));
        verify(awsEC2Platform, never()).lookupChaosSecurityGroup(vpcId);
    }

    @Test
    public void createChaosSecurityGroup () {
        String vpcId = randomUUID().toString();
        String securityGroupId = randomUUID().toString();
        ArgumentCaptor<CreateSecurityGroupRequest> createSecurityGroupRequestArgumentCaptor = ArgumentCaptor.forClass(CreateSecurityGroupRequest.class);
        doReturn(new CreateSecurityGroupResult().withGroupId(securityGroupId)).when(amazonEC2)
                                                                              .createSecurityGroup(createSecurityGroupRequestArgumentCaptor
                                                                                      .capture());
        assertEquals(securityGroupId, awsEC2Platform.createChaosSecurityGroup(vpcId));
        CreateSecurityGroupRequest request = createSecurityGroupRequestArgumentCaptor.getValue();
        assertEquals(AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_NAME + "-" + vpcId, request.getGroupName());
        assertEquals(vpcId, request.getVpcId());
        assertEquals(AwsEC2Constants.EC2_DEFAULT_CHAOS_SECURITY_GROUP_DESCRIPTION, request.getDescription());
        ArgumentCaptor<RevokeSecurityGroupEgressRequest> revokeSecurityGroupEgressRequestArgumentCaptor = ArgumentCaptor
                .forClass(RevokeSecurityGroupEgressRequest.class);
        verify(amazonEC2, times(1)).revokeSecurityGroupEgress(revokeSecurityGroupEgressRequestArgumentCaptor.capture());
        RevokeSecurityGroupEgressRequest revokeSecurityGroupEgressRequest = revokeSecurityGroupEgressRequestArgumentCaptor
                .getValue();
        assertEquals(Collections.singletonList(AwsEC2Constants.DEFAULT_IP_PERMISSIONS), revokeSecurityGroupEgressRequest
                .getIpPermissions());
    }

    @Test
    public void lookupChaosSecurityGroup () {
        String vpcId = randomUUID().toString();
        String groupId = randomUUID().toString();
        ArgumentCaptor<DescribeSecurityGroupsRequest> captor = ArgumentCaptor.forClass(DescribeSecurityGroupsRequest.class);
        doReturn(groupId).when(awsEC2Platform).createChaosSecurityGroup(vpcId);
        doReturn(new DescribeSecurityGroupsResult()).when(amazonEC2).describeSecurityGroups(captor.capture());
        assertEquals(groupId, awsEC2Platform.lookupChaosSecurityGroup(vpcId));
        verify(awsEC2Platform, times(1)).createChaosSecurityGroup(vpcId);
        assertThat(captor.getValue().getFilters(), IsIterableContainingInAnyOrder.containsInAnyOrder(new Filter("vpc-id").withValues(vpcId)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInvalidSecurityGroup () {
        ArgumentCaptor<Collection<String>> groupListCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<ModifyNetworkInterfaceAttributeRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyNetworkInterfaceAttributeRequest.class);
        List<String> securityGroups = IntStream.range(0, 10).mapToObj(i -> randomUUID().toString()).collect(Collectors.toList());
        String instanceId = randomUUID().toString();
        AmazonEC2Exception exception = new AmazonEC2Exception("Test Exception");
        exception.setErrorCode(AwsEC2Constants.SECURITY_GROUP_NOT_FOUND);
        doThrow(exception).when(amazonEC2).modifyNetworkInterfaceAttribute(modifyRequestCaptor.capture());
        doNothing().when(awsEC2Platform).processInvalidGroups(groupListCaptor.capture());
        try {
            awsEC2Platform.setSecurityGroupIds(instanceId, securityGroups);
            fail("Expected a ChaosException");
        } catch (ChaosException ignored) {
            // Catching the exception so it doesn't throw up, and the above Fail doesn't trigger either.
        }
        Collection<String> groupsToRemove = groupListCaptor.getValue();
        ModifyNetworkInterfaceAttributeRequest modifyRequest = modifyRequestCaptor.getValue();
        assertEquals(instanceId, modifyRequest.getNetworkInterfaceId());
        assertThat(modifyRequest.getGroups(), IsIterableContainingInAnyOrder.containsInAnyOrder(securityGroups.toArray(new String[0])));
        assertThat(groupsToRemove, IsIterableContainingInAnyOrder.containsInAnyOrder(securityGroups.toArray(new String[0])));
    }

    @Test
    public void setInvalidSecurityGroupWithDifferentError () {
        ArgumentCaptor<ModifyNetworkInterfaceAttributeRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyNetworkInterfaceAttributeRequest.class);
        List<String> securityGroups = IntStream.range(0, 10).mapToObj(i -> randomUUID().toString()).collect(Collectors.toList());
        String instanceId = randomUUID().toString();
        AmazonEC2Exception exception = new AmazonEC2Exception("Test Exception");
        doThrow(exception).when(amazonEC2).modifyNetworkInterfaceAttribute(modifyRequestCaptor.capture());
        try {
            awsEC2Platform.setSecurityGroupIds(instanceId, securityGroups);
            fail("Expected a ChaosException");
        } catch (ChaosException ignored) {
            // Catching the exception so it doesn't throw up, and the above Fail doesn't trigger either.
        }
        verify(awsEC2Platform, never()).processInvalidGroups(any());
        ModifyNetworkInterfaceAttributeRequest modifyRequest = modifyRequestCaptor.getValue();
        assertEquals(instanceId, modifyRequest.getNetworkInterfaceId());
        assertThat(modifyRequest.getGroups(), IsIterableContainingInAnyOrder.containsInAnyOrder(securityGroups.toArray(new String[0])));
    }

    @Test
    public void processInvalidGroups () {
        Map<String, String> vpcToSecurityGroupMap;
        Collection<String> validSecurityGroups = IntStream.range(0, 10).mapToObj(i -> randomUUID().toString()).collect(Collectors.toList());
        String invalidSecurityGroup = randomUUID().toString();
        String invalidVpc = randomUUID().toString();
        validSecurityGroups.forEach(s -> {
            String vpc = randomUUID().toString();
            String instanceId = randomUUID().toString();
            doReturn(vpc).when(awsEC2Platform).getVpcIdOfContainer(instanceId);
            doReturn(s).when(awsEC2Platform).lookupChaosSecurityGroup(vpc);
            awsEC2Platform.getChaosSecurityGroupForInstance(instanceId);
        });
        String instanceId = randomUUID().toString();
        doReturn(invalidVpc).when(awsEC2Platform).getVpcIdOfContainer(instanceId);
        doReturn(invalidSecurityGroup).when(awsEC2Platform).lookupChaosSecurityGroup(invalidVpc);
        awsEC2Platform.getChaosSecurityGroupForInstance(instanceId);
        // Lookup Security Group Map and make sure it's valid before we start pruning
        vpcToSecurityGroupMap = awsEC2Platform.getVpcToSecurityGroupMap();
        Collection<String> expectedSecurityGroups = new ArrayList<>(validSecurityGroups);
        expectedSecurityGroups.add(invalidSecurityGroup);
        assertThat(vpcToSecurityGroupMap.values(), IsIterableContainingInAnyOrder.containsInAnyOrder(expectedSecurityGroups
                .toArray(new String[0])));
        // Prune out the security group and make sure it's different.
        awsEC2Platform.processInvalidGroups(Collections.singleton(invalidSecurityGroup));
        vpcToSecurityGroupMap = awsEC2Platform.getVpcToSecurityGroupMap();
        assertThat(vpcToSecurityGroupMap.values(), IsIterableContainingInAnyOrder.containsInAnyOrder(validSecurityGroups
                .toArray(new String[0])));
    }

    @Test
    public void getVpcToSecurityGroupMap () {
        Map<String, String> firstMap = awsEC2Platform.getVpcToSecurityGroupMap();
        Map<String, String> secondMap = awsEC2Platform.getVpcToSecurityGroupMap();
        assertNotSame("The VPC To Security Group Map should return a clone, not the same object", firstMap, secondMap);
    }

    @Test
    public void getUsernameForImageId () {
        awsEC2Platform.setImageIdToUsernameMap(Map.of("rhel", "root", "amazon-linux", "ec2-user"));
        assertEquals("root", awsEC2Platform.getUsernameForImageId("rhel"));
        assertEquals("ec2-user", awsEC2Platform.getUsernameForImageId("amazon-linux"));
        assertEquals("ec2-user", awsEC2Platform.getUsernameForImageId("centos"));
    }

    @Test(expected = Test.None.class /* No exception expected */)
    public void routableCidrBlockNoException () {
        Collection<String> cidrBlocks = Set.of("192.168.1.0/24", "172.16.0.0/16", "10.0.0.0/8");
        awsEC2Platform.setRoutableCidrBlocks(cidrBlocks);
    }

    @Test
    public void routableCidrBlockWithException () {
        Collection<String> cidrBlocks = Set.of("testFailure");
        try {
            awsEC2Platform.setRoutableCidrBlocks(cidrBlocks);
            fail();
        } catch (ChaosException e) {
            assertTrue(e.getMessage().startsWith("21103"));
        }
    }

    @Test
    public void isAddressRoutable () {
        final SubnetUtils.SubnetInfo privateBlock1 = new SubnetUtils("10.0.0.0/8").getInfo();
        final SubnetUtils.SubnetInfo privateBlock2 = new SubnetUtils("172.16.0.0/16").getInfo();
        final SubnetUtils.SubnetInfo privateBlock3 = new SubnetUtils("192.168.1.0/24").getInfo();
        doReturn(Set.of(privateBlock1, privateBlock2), Set.of(privateBlock2, privateBlock3), Set.of(privateBlock3, privateBlock1))
                .when(awsEC2Platform)
                .getRoutableCidrBlocks();
        assertTrue(awsEC2Platform.isAddressRoutable("172.16.1.192"));
        assertFalse(awsEC2Platform.isAddressRoutable("10.1.2.3"));
        assertTrue(awsEC2Platform.isAddressRoutable("10.1.2.3"));
        assertFalse(awsEC2Platform.isAddressRoutable("fake"));
    }

    @Test
    public void isContainerStarted () {
        AwsEC2Container awsEC2Container = mock(AwsEC2Container.class);
        String instanceId = randomUUID().toString();
        doReturn(instanceId).when(awsEC2Container).getInstanceId();
        Instance instance = new Instance().withInstanceId(instanceId).withState(new InstanceState().withCode(16));
        Reservation reservation = new Reservation().withInstances(instance);
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservation);
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertTrue("Container is running, should be true", awsEC2Platform.isStarted(awsEC2Container));
    }

    @Test
    public void isContainerStarted2 () {
        AwsEC2Container awsEC2Container = mock(AwsEC2Container.class);
        String instanceId = randomUUID().toString();
        doReturn(instanceId).when(awsEC2Container).getInstanceId();
        Instance instance = new Instance().withInstanceId(instanceId).withState(new InstanceState().withCode(48));
        Reservation reservation = new Reservation().withInstances(instance);
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservation);
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertFalse("Container isn't started, should be false", awsEC2Platform.isStarted(awsEC2Container));
    }

    @Test
    public void isContainerStarted3 () {
        AwsEC2Container awsEC2Container = mock(AwsEC2Container.class);
        String instanceId = randomUUID().toString();
        doReturn(instanceId).when(awsEC2Container).getInstanceId();
        Reservation reservation = new Reservation();
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservation);
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertFalse("No container returned, should be false", awsEC2Platform.isStarted(awsEC2Container));
    }

    @Test
    public void generateRosterWithIncompleteInstances () {
        Instance firstIncompleteInstance = new Instance().withPlacement(new Placement(null));
        Instance secondIncompleteInstance = new Instance().withPlacement(null);
        Instance completedInstance = new Instance().withPlacement(new Placement("us-east-1b"));
        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(new Reservation()
                .withInstances(firstIncompleteInstance, secondIncompleteInstance, completedInstance));
        doReturn(describeInstancesResult).when(amazonEC2).describeInstances(any());
        AwsEC2Container completedInstanceContainer = AwsEC2Container.builder().build();
        doReturn(completedInstanceContainer).when(awsEC2Platform).createContainerFromInstance(completedInstance);
        assertThat(awsEC2Platform.generateRoster(), containsInAnyOrder(completedInstanceContainer));
        verify(awsEC2Platform, never()).createContainerFromInstance(firstIncompleteInstance);
        verify(awsEC2Platform, never()).createContainerFromInstance(secondIncompleteInstance);
    }

    @Test
    public void getNetworkInterfaceToSecurityGroupMap () {
        String instanceId = "this is my instance id string";
        NetworkInterface networkInterface1, networkInterface2;
        GroupIdentifier group1a, group1b, group2a, group2b;
        group1a = new GroupIdentifier().withGroupId("group-1-a");
        group1b = new GroupIdentifier().withGroupId("group-1-b");
        group2a = new GroupIdentifier().withGroupId("group-2-a");
        group2b = new GroupIdentifier().withGroupId("group-2-b");
        networkInterface1 = new NetworkInterface().withNetworkInterfaceId("network-interface-a")
                                                  .withGroups(group1a, group1b);
        networkInterface2 = new NetworkInterface().withNetworkInterfaceId("network-interface-b")
                                                  .withGroups(group2a, group2b);
        ArgumentCaptor<DescribeNetworkInterfacesRequest> captor = ArgumentCaptor.forClass(DescribeNetworkInterfacesRequest.class);
        doReturn(new DescribeNetworkInterfacesResult().withNetworkInterfaces(networkInterface1, networkInterface2)).when(amazonEC2)
                                                                                                                   .describeNetworkInterfaces(captor
                                                                                                                           .capture());
        Map<String, Set<String>> networkInterfaceToSecurityGroupsMap = awsEC2Platform.getNetworkInterfaceToSecurityGroupsMap(instanceId);
        assertThat(captor.getValue()
                         .getFilters(), containsInAnyOrder(new Filter("attachment.instance-id", List.of(instanceId))));
        Map expected = Map.of("network-interface-a", Set.of("group-1-a", "group-1-b"), "network-interface-b", Set.of("group-2-a", "group-2-b"));
        assertEquals(expected, networkInterfaceToSecurityGroupsMap);
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private AmazonEC2 amazonEC2;
        @Autowired
        private ContainerManager containerManager;
        @Autowired
        private AwsEC2SelfAwareness awsEC2SelfAwareness;

        @Bean
        AwsEC2Platform awsEC2Platform () {
            return Mockito.spy(new AwsEC2Platform(amazonEC2, containerManager, awsEC2SelfAwareness));
        }
    }
}