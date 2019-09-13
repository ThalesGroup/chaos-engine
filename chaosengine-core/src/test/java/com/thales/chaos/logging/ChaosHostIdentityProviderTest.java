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

package com.thales.chaos.logging;

import com.thales.chaos.util.AwsMetadataUtil;
import com.thales.chaos.util.GoogleCloudMetadataUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChaosHostIdentityProviderTest {
    @Spy
    private ChaosHostIdentityProvider chaosHostIdentityProvider;

    @Before
    public void setUp () {
        doReturn("personalIdentifier").when(chaosHostIdentityProvider).getPersonalIdentifier();
        doReturn("network").when(chaosHostIdentityProvider).getNetworkIdentifier();
        doReturn("hostname").when(chaosHostIdentityProvider).getHostName();
        doReturn("staging").when(chaosHostIdentityProvider).getKubernetesNamespace();
    }

    @Test
    public void getPropertyValueFull () {
        assertEquals("personalIdentifier@network", chaosHostIdentityProvider.getPropertyValue());
        verify(chaosHostIdentityProvider, never()).getHostName();
    }

    @Test
    public void getPropertyValueNullPersonalIdentifier () {
        doReturn(null).when(chaosHostIdentityProvider).getPersonalIdentifier();
        assertEquals("network", chaosHostIdentityProvider.getPropertyValue());
        verify(chaosHostIdentityProvider, never()).getHostName();
    }

    @Test
    public void getPropertyValueNullNetworkIdentifier () {
        doReturn(null).when(chaosHostIdentityProvider).getNetworkIdentifier();
        assertEquals("personalIdentifier", chaosHostIdentityProvider.getPropertyValue());
        verify(chaosHostIdentityProvider, never()).getHostName();
    }

    @Test
    public void getPropertyValueNullPersonalAndNetworkIdentifier () {
        doReturn(null).when(chaosHostIdentityProvider).getPersonalIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getNetworkIdentifier();
        assertEquals("hostname", chaosHostIdentityProvider.getPropertyValue());
    }

    @Test
    public void getPersonalIdentifierForKubernetesNode () {
        doCallRealMethod().when(chaosHostIdentityProvider).getPersonalIdentifier();
        assertEquals("k8s:hostname.staging", chaosHostIdentityProvider.getPersonalIdentifier());
    }

    @Test
    public void getPersonalIdentifierForDefaultNode () {
        doCallRealMethod().when(chaosHostIdentityProvider).getPersonalIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getKubernetesIdentifier();
        assertEquals("hostname", chaosHostIdentityProvider.getPersonalIdentifier());
    }

    @Test
    public void getKubernetesIdentifier () {
        doCallRealMethod().when(chaosHostIdentityProvider).getKubernetesIdentifier();
        assertEquals("k8s:hostname.staging", chaosHostIdentityProvider.getKubernetesIdentifier());
    }

    @Test
    public void getKubernetesIdentifierWithNullNamespace () {
        doCallRealMethod().when(chaosHostIdentityProvider).getKubernetesIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getKubernetesNamespace();
        assertNull(chaosHostIdentityProvider.getKubernetesIdentifier());
    }

    @Test
    public void getKubernetesIdentifierWithNullHostname () {
        doCallRealMethod().when(chaosHostIdentityProvider).getKubernetesIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getHostName();
        assertEquals("k8s:staging", chaosHostIdentityProvider.getKubernetesIdentifier());
    }

    @Test
    public void getAwsIdentifierWithNullIdentifier () {
        doReturn(null).when(chaosHostIdentityProvider).getAwsInstanceIdentity();
        assertNull(chaosHostIdentityProvider.getAwsIdentifier());
    }

    @Test
    public void getAwsIdentifier () {
        AwsMetadataUtil.AwsInstanceIdentity identity = mock(AwsMetadataUtil.AwsInstanceIdentity.class);
        doReturn(identity).when(chaosHostIdentityProvider).getAwsInstanceIdentity();
        doReturn("i-123456abcdef").when(identity).getInstanceId();
        doReturn("012345678901").when(identity).getAccountId();
        doReturn("us-west-2").when(identity).getRegion();
        assertEquals("aws:i-123456abcdef:012345678901:us-west-2", chaosHostIdentityProvider.getAwsIdentifier());
    }

    @Test
    public void getNetworkIdentifierWithAwsIdentifier () {
        doCallRealMethod().when(chaosHostIdentityProvider).getNetworkIdentifier();
        doReturn("aws:i-123456abcdef:012345678901:us-west-2").when(chaosHostIdentityProvider).getAwsIdentifier();
        assertEquals("aws:i-123456abcdef:012345678901:us-west-2", chaosHostIdentityProvider.getNetworkIdentifier());
        verify(chaosHostIdentityProvider, never()).getIpAddress();
    }

    @Test
    public void getNetworkIdentifierWithGoogleIdentifier () {
        doCallRealMethod().when(chaosHostIdentityProvider).getNetworkIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getAwsIdentifier();
        doReturn("gcp:kubelet:project/12345/zone/us-central1-a").when(chaosHostIdentityProvider)
                                                                .getGoogleCloudIdentifier();
        assertEquals("gcp:kubelet:project/12345/zone/us-central1-a", chaosHostIdentityProvider.getNetworkIdentifier());
    }

    @Test
    public void getNetworkIdentifierWithIpAddress () {
        doCallRealMethod().when(chaosHostIdentityProvider).getNetworkIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getAwsIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getGoogleCloudIdentifier();
        doReturn("1.2.3.4").when(chaosHostIdentityProvider).getIpAddress();
        assertEquals("1.2.3.4", chaosHostIdentityProvider.getNetworkIdentifier());
    }

    @Test
    public void getNetworkIdentifierWithNullEverything () {
        doCallRealMethod().when(chaosHostIdentityProvider).getNetworkIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getAwsIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getGoogleCloudIdentifier();
        doReturn(null).when(chaosHostIdentityProvider).getIpAddress();
        assertNull(chaosHostIdentityProvider.getNetworkIdentifier());
    }

    @Test
    public void getGoogleCloudIdentifier () {
        GoogleCloudMetadataUtil.GoogleCloudInstanceIdentity identity = mock(GoogleCloudMetadataUtil.GoogleCloudInstanceIdentity.class);
        doReturn(identity).when(chaosHostIdentityProvider).getGoogleIdentity();
        doReturn("kubelet-12345").when(identity).getName();
        doReturn("project/123456789/zone/us-central1-a").when(identity).getZone();
        assertEquals("gcp:kubelet-12345:project/123456789/zone/us-central1-a", chaosHostIdentityProvider.getGoogleCloudIdentifier());
    }
}