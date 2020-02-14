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

package com.thales.chaos.shellclient.ssh;

import org.hamcrest.collection.IsMapWithSize;
import org.junit.Before;
import org.junit.Test;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

public class GcpRuntimeSSHKeyTest {
    private GcpRuntimeSSHKey gcpRuntimeSSHKey;

    @Before
    public void setUp () {
        gcpRuntimeSSHKey = spy(new GcpRuntimeSSHKey());
    }

    @Test
    public void fieldNullCheck () {
        assertNotNull(gcpRuntimeSSHKey.getSshKeyMetadata());
        assertNotNull(gcpRuntimeSSHKey.getUsername());
        assertNotNull(gcpRuntimeSSHKey.getSSHKeys());
        assertNull("GCP does not use passwords, should not have a password generator",
                gcpRuntimeSSHKey.getPasswordGenerator());
    }

    @Test
    public void getSshKeyMetadata () {
        GcpSSHKeyMetadata actual = gcpRuntimeSSHKey.getSshKeyMetadata();
        assertTrue(gcpRuntimeSSHKey.getSSHKeys()
                                   .keySet()
                                   .stream()
                                   .map(Key::getEncoded)
                                   .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                                   .anyMatch(s -> actual.getPublicKey().equals(s)));
        assertEquals("chaosengine", actual.getUsername());
    }

    @Test
    public void keyPairWorks () {
        Map<PublicKey, PrivateKey> sshKeys = gcpRuntimeSSHKey.getSSHKeys();
        sshKeys.forEach((pub, priv) -> {
            try {
                validateKeyPair(priv, pub);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        assertThat(sshKeys, IsMapWithSize.aMapWithSize(1));
    }

    private void validateKeyPair (PrivateKey privateKey, PublicKey publicKey) throws Exception {
        byte[] challenge = new byte[1024 * 1024];
        new Random().nextBytes(challenge);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge);
        byte[] signatureBytes = signature.sign();
        signature.initVerify(publicKey);
        signature.update(challenge);
        assertTrue("KeyPair did not validate challenge/response", signature.verify(signatureBytes));
    }

}