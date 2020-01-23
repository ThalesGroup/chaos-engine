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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.thales.chaos.constants.SSHConstants;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import net.schmizz.sshj.userauth.method.AuthMethod;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@JsonIgnoreType
public interface SSHCredentials {
    static PrivateKey privateKeyFromString (String privateKey) {
        try {
            String base64Key = privateKey.replaceAll("-*(BEGIN|END) (RSA )?PRIVATE KEY-*", "").replaceAll("\n", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ChaosException(ChaosErrorCode.SSH_CREDENTIALS_INVALID_KEY_FORMAT, e);
        }
    }

    static PublicKey publicKeyFromPrivateKey (PrivateKey privateKey, String publicKey) {
        try {
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), publicKey == null ? SSHConstants.DEFAULT_RSA_PUBLIC_EXPONENT : getExponentFromPublicKey(publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ChaosException(ChaosErrorCode.SSH_CREDENTIALS_INVALID_KEY_FORMAT, e);
        }
    }

    static BigInteger getExponentFromPublicKey (String publicKey) {
        throw new UnsupportedOperationException("This is a placeholder for if we need to work with keys with Public Exponent that is not 65537.");
    }

    String getUsername ();

    Callable<String> getPasswordGenerator ();

    Map<PublicKey, PrivateKey> getSSHKeys ();

    boolean isSupportSudo ();

    SSHCredentials withUsername (String username);

    SSHCredentials withPasswordGenerator (Callable<String> passwordGenerator);

    SSHCredentials withKeyPair (String privateKey, String publicKey);

    SSHCredentials withKeyPair (PrivateKey privateKey, PublicKey publicKey);

    SSHCredentials withSupportSudo (boolean supportSudo);

    List<AuthMethod> getAuthMethods ();
}
