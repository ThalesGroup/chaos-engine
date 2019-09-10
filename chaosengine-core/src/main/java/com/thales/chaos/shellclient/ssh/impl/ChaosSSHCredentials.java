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

package com.thales.chaos.shellclient.ssh.impl;

import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.shellclient.ssh.SSHCredentials;
import net.schmizz.sshj.userauth.keyprovider.KeyPairWrapper;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPassword;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.thales.chaos.exception.enums.ChaosErrorCode.SSH_CREDENTIAL_PASSWORD_CALL_FAILURE;

public class ChaosSSHCredentials implements SSHCredentials {
    private static final Logger log = LoggerFactory.getLogger(ChaosSSHCredentials.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Map<PublicKey, PrivateKey> sshKeys = new HashMap<>();
    private String username;
    private Callable<String> passwordGenerator;
    private boolean supportSudo = true;

    @Override
    public String getUsername () {
        return username;
    }

    @Override
    public Callable<String> getPasswordGenerator () {
        return passwordGenerator;
    }

    @Override
    public Map<PublicKey, PrivateKey> getSSHKeys () {
        return sshKeys;
    }

    @Override
    public boolean isSupportSudo () {
        return supportSudo;
    }

    @Override
    public SSHCredentials withUsername (String username) {
        this.username = username;
        return this;
    }

    @Override
    public SSHCredentials withPasswordGenerator (Callable<String> passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
        return this;
    }

    @Override
    public SSHCredentials withKeyPair (String privateKey, String publicKey) {
        PrivateKey privKey = SSHCredentials.privateKeyFromString(privateKey);
        return withKeyPair(privKey, SSHCredentials.publicKeyFromPrivateKey(privKey, publicKey));
    }

    @Override
    public SSHCredentials withKeyPair (PrivateKey privateKey, PublicKey publicKey) {
        sshKeys.put(publicKey, privateKey);
        return this;
    }

    @Override
    public SSHCredentials withSupportSudo (boolean supportSudo) {
        this.supportSudo = supportSudo;
        return this;
    }

    @Override
    public List<AuthMethod> getAuthMethods () {
        List<AuthMethod> authMethods = sshKeys.entrySet()
                                              .stream()
                                              .filter(e -> e.getKey() != null && e.getValue() != null)
                                              .map(e -> new KeyPairWrapper(e.getKey(), e.getValue()))
                                              .map(AuthPublickey::new)
                                              .collect(Collectors.toList());
        if (passwordGenerator != null) {
            try {
                authMethods.add(new AuthPassword(new PasswordFinder() {
                    @Override
                    public char[] reqPassword (Resource<?> resource) {
                        try {
                            return passwordGenerator.call().toCharArray();
                        } catch (Exception e) {
                            throw new ChaosException(SSH_CREDENTIAL_PASSWORD_CALL_FAILURE, e);
                        }
                    }

                    @Override
                    public boolean shouldRetry (Resource<?> resource) {
                        return false;
                    }
                }));
            } catch (ChaosException e) {
                log.error("Could not create password-based authentication token", e);
            }
        }
        return authMethods;
    }
}
