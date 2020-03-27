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

import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.shellclient.ssh.impl.ChaosSSHCredentials;
import net.schmizz.sshj.common.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.util.Base64;

import static com.thales.chaos.exception.enums.GcpComputeChaosErrorCode.GCP_COMPUTE_KEY_CREATION_ERROR;
import static net.logstash.logback.argument.StructuredArguments.v;

public class GcpRuntimeSSHKey extends ChaosSSHCredentials {
    public static final String CHAOS_USERNAME = "chaosengine";
    private static final Logger log = LoggerFactory.getLogger(GcpRuntimeSSHKey.class);
    private static final String CHAOS_SSH_IDENTIFIER = "chaos@anytime";
    private GcpSSHKeyMetadata sshKeyMetadata;

    public GcpRuntimeSSHKey () {
        try {
            constructKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new ChaosException(GCP_COMPUTE_KEY_CREATION_ERROR, e);
        }
    }

    void constructKey () throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = generateKeyPair();
        this.withUsername(CHAOS_USERNAME).withKeyPair(keyPair.getPrivate(), keyPair.getPublic());
        byte[] compactData = new Buffer.PlainBuffer().putPublicKey(keyPair.getPublic()).getCompactData();
        String b64Key = Base64.getEncoder().encodeToString(compactData);
        sshKeyMetadata = new GcpSSHKeyMetadata(CHAOS_USERNAME, b64Key, CHAOS_SSH_IDENTIFIER);
        log.info("Created SSH Key for use in GCP with public key {}", v("publicKey", sshKeyMetadata));
    }

    KeyPair generateKeyPair () throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA", "BC");
        rsa.initialize(2048, new SecureRandom());
        return rsa.generateKeyPair();
    }

    public GcpSSHKeyMetadata getSshKeyMetadata () {
        return sshKeyMetadata;
    }
}
