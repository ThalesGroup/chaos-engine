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

import com.thales.chaos.shellclient.ssh.impl.ChaosSSHCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import static net.logstash.logback.argument.StructuredArguments.v;

public class GcpRuntimeSSHKey extends ChaosSSHCredentials {
    private static final Logger log = LoggerFactory.getLogger(GcpRuntimeSSHKey.class);
    private static final String CHAOS_USERNAME = "chaosengine";
    private static final String CHAOS_SSH_IDENTIFIER = "chaos@anytime";
    private GcpSSHKeyMetadata sshKeyMetadata;

    public GcpSSHKeyMetadata getSshKeyMetadata () {
        return sshKeyMetadata;
    }

    @Autowired
    public void constructKey () throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        this.withUsername(CHAOS_USERNAME).withKeyPair(keyPair.getPrivate(), keyPair.getPublic());
        String b64Key = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        sshKeyMetadata = new GcpSSHKeyMetadata(CHAOS_USERNAME, b64Key, CHAOS_SSH_IDENTIFIER);
        log.info("Created SSH Key for use in GCP with public key {}", v("publicKey", sshKeyMetadata));
    }

    KeyPair generateKeyPair () throws NoSuchAlgorithmException {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(1024, new SecureRandom());
        return rsa.generateKeyPair();
    }
}
