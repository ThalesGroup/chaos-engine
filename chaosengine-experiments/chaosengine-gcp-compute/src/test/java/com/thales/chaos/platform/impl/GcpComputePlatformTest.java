/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

import com.google.cloud.compute.v1.*;
import com.thales.chaos.constants.GcpConstants;
import com.thales.chaos.container.impl.GcpComputeInstanceContainer;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.thales.chaos.services.impl.GcpComputeService.COMPUTE_PROJECT;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class GcpComputePlatformTest {
    private static final String MY_AWESOME_PROJECT = "my-awesome-project";
    @MockBean
    private InstanceClient instanceClient;
    @Autowired
    private ProjectName projectName;
    @Autowired
    private GcpComputePlatform gcpComputePlatform;

    @Test
    public void isIAASPlatform () {
        assertEquals(PlatformLevel.IAAS, gcpComputePlatform.getPlatformLevel());
    }

    @Test
    public void createContainerFromInstanceWithNoDetails () {
        Instance instance = Instance.newBuilder().build();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder().build();
        assertEquals(container, gcpComputePlatform.createContainerFromInstance(instance));
    }

    @Test
    public void createContainerFromInstanceWithDetails () {
        String createdByValue = "My-VM-Instance-Group";
        Metadata metadata = Metadata.newBuilder()
                                    .addItems(Items.newBuilder()
                                                   .setKey(GcpConstants.CREATED_BY_METADATA_KEY)
                                                   .setValue(createdByValue)
                                                   .build()).build();
        String tag1 = "HTTP";
        String tag2 = "SSH";
        String tag3 = "RDP";
        Tags tags = Tags.newBuilder().addAllItems(List.of(tag1, tag2, tag3)).build();
        String id = "123456789101112131415";
        String name = "My-Weird-SSH-and-RDP-Web-Server";
        String zone = "some-datacenter-somewhere";
        Instance instance = Instance.newBuilder()
                                    .setMetadata(metadata)
                                    .setName(name)
                                    .setId(id)
                                    .setTags(tags)
                                    .setZone(zone)
                                    .build();
        GcpComputeInstanceContainer container = GcpComputeInstanceContainer.builder()
                                                                           .withCreatedBy(createdByValue)
                                                                           .withFirewallTags(List.of(tag1, tag2, tag3))
                                                                           .withPlatform(gcpComputePlatform)
                                                                           .withInstanceName(name)
                                                                           .withUniqueIdentifier(id)
                                                                           .withZone(zone)
                                                                           .build();
        assertEquals(container, gcpComputePlatform.createContainerFromInstance(instance));
    }

    @Test
    public void generateRoster () {
        GcpComputeInstanceContainer expected = GcpComputeInstanceContainer.builder()
                                                                          .withUniqueIdentifier("12345678901234567890")
                                                                          .build();
        InstanceClient.AggregatedListInstancesPagedResponse response = mock(InstanceClient.AggregatedListInstancesPagedResponse.class);
        Instance instance = Instance.newBuilder().setId("12345678901234567890").build();
        Iterable<InstancesScopedList> iterableInstances = List.of(InstancesScopedList.newBuilder()
                                                                                     .addInstances(instance)
                                                                                     .build());
        doReturn(iterableInstances).when(response).iterateAll();
        doReturn(response).when(gcpComputePlatform).getAggregatedInstanceList();
        assertThat(gcpComputePlatform.generateRoster(), containsInAnyOrder(expected));
    }

    @Configuration
    public static class GcpComputePlatformTestConfiguration {
        @Autowired
        private InstanceClient instanceClient;

        @Bean(name = COMPUTE_PROJECT)
        public ProjectName projectName () {
            return ProjectName.of(MY_AWESOME_PROJECT);
        }

        @Bean
        public GcpComputePlatform gcpComputePlatform () {
            return spy(new GcpComputePlatform());
        }
    }
}