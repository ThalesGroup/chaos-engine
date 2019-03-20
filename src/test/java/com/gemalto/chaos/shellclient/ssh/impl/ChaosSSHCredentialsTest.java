package com.gemalto.chaos.shellclient.ssh.impl;

import com.gemalto.chaos.shellclient.ssh.SSHCredentials;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPassword;
import org.hamcrest.collection.IsIterableWithSize;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gemalto.chaos.shellclient.ssh.SSHCredentials.privateKeyFromString;
import static com.gemalto.chaos.shellclient.ssh.SSHCredentials.publicKeyFromPrivateKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class ChaosSSHCredentialsTest {
    private SSHCredentials sshCredentials;
    private String PUBLIC_KEY = null;
    // This private key was generated explicitly for this test and is not used anywhere
    private String PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" + "MIIEpgIBAAKCAQEAuENqLqKYT7vld6EvSK1myOH29dX2lb3sLEXcHybgKGr1kjjU\n" + "OfqUBDjRnhFdc9i4dA6AwBI71owWKXHrPWWz6BT/hThxGh9oHUwzhW+0tPlmugBt\n" + "PA9mK5fowvalujPyxp+RJxQO7focZVkb8DmSL+mGHzozPPcbgk+AKULVzpiAEQXn\n" + "cfjPxsHEjzjgWCrobValf7GNEG0oBnV7AaT4nSNGjiHEOpatacmKB7lsdfugXkau\n" + "CIwM+PSaMejbIPfbcfb4j18PrH5YuXDhVvkVS6d65UvzNG7yZ5P1+s/BSv7qgTba\n" + "c3EoYFftHeR7Cbr4Ul57BaOunVYEIh3fVE4Z/QIDAQABAoIBAQCln1rivh0/lflk\n" + "tQnGPq5jo9ASGhcBc2vEQ6reiVf0nGdM3i3MS3Id9WBcvukMBuGM17hcbEdCyjnl\n" + "WNMEJdU2pyEhgqEOxOoVY3fv2X9JJ99xEo8c7A5+4pVCIObH3oe6hpS21m2w9B3v\n" + "7s+Q9n5y+GkoymxhwQ8F6yWccMmzXpLQ2d0qtKKwT/6Ax/Gi7iGSM7+syauKOZUq\n" + "X1oyrnqJarUU7pDlfXWNI+5+8QtsGdiWPtSmW6KJcLlgJe7hLj+/VvgT5S4eFZkP\n" + "WEZ55l5Ho+hj3Q77t5sVEi5vPFni8OzxhVZ/gJuMnA/idnUdL7eoIYxlhncd2amk\n" + "UwDSHnlhAoGBAOSnR97FZ024ML2CRnyAgZVI24xDpSr7eb7RcX69zgJ4CqbkhUTZ\n" + "qT/OkhX0IEn9sd/nMb8YMDehiRoVUxkEY0P66szpuYNMeSXcRKsHxl8h6tLL6MdF\n" + "T7lUHQNGVDaf5zWDMKIM8bjAFbOrRaeqAgDIeXR8WJJ3apnM89APKNE1AoGBAM5N\n" + "CP69y89desKilcCeKaexjulpIohMr4OqKxUXtG7TJmyDftwXppu3o/8dBIFs+qRB\n" + "hriD5Oj7/J8kNwX3jI1bwKoHYQXvggccQEKf7DDijkPFsp5TeS/Z6n0Q2qz/x9pS\n" + "MFcNX3w5QEeYce9gK77UnbHwo05MM/eHYuO0+capAoGBANh+msoO1rT7xQpqIxsX\n" + "AZq7lUTFd8muuaM24+NBC6HCzzMeBCEghePoTuGCeGAWWSjK2os49850oD+WGUwC\n" + "n4pqNY83bXMoITz7xfi6L9U7Y/+bGhgzRD0RqzbCjFH8LQq54H5PC0AGf4XSkO0j\n" + "Ryo+puDoK2TitIhTSjahY+BRAoGBAJbyJrK9jHKdo0TmUUwubawVbsu58VppQcLE\n" + "r+EvBwZP2L1tOVUwuOcW5xey06kfZATrLPe1CbivJ5gJl3j2OBD2IXJHE+aT6D/r\n" + "m9kjyl26Zv2PmbHewb0RZVE9E8OhI0nK+TO4xTTPbwjzsenEJD3ss7WOrO6NMzja\n" + "BrE1qcyBAoGBAI6i5nR024NGc7/UNhQ1P7PyrrzHMDujwQ3QGZ6fv7w0sJl8YVK7\n" + "8lxS98tyfmFXeqtbQ+pUYZyMBr3IMicumhW9frnPP7W9GJzyOAgCWUwr4+2Dqy+P\n" + "cxsbp/4QHL+kwxzKqF6w3s6ZQ5sOh8vBoUf3RhdjM7NY7dQOHhUltfN6\n" + "-----END RSA PRIVATE KEY-----\n";
    private String USERNAME = "sshUsername";
    private Callable<String> PASSWORD_GENERATOR = () -> "sshPassword";

    @Before
    public void setUp () {
        sshCredentials = new ChaosSSHCredentials().withUsername(USERNAME)
                                                  .withKeyPair(PRIVATE_KEY, PUBLIC_KEY)
                                                  .withPasswordGenerator(PASSWORD_GENERATOR);
    }

    @Test
    public void getUsername () {
        assertEquals(USERNAME, sshCredentials.getUsername());
    }

    @Test
    public void getPasswordGenerator () {
        assertSame(PASSWORD_GENERATOR, sshCredentials.getPasswordGenerator());
    }

    @Test
    public void getSSHKeys () {
        PrivateKey privateKey = privateKeyFromString(PRIVATE_KEY);
        PublicKey publicKey = publicKeyFromPrivateKey(privateKey, null);
        assertEquals(Collections.singletonMap(publicKey, privateKey), sshCredentials.getSSHKeys());
    }

    @Test
    public void getAuthMethods () {
        final List<AuthMethod> authMethods = sshCredentials.getAuthMethods();
        assertThat(authMethods, IsIterableWithSize.iterableWithSize(2));
        for (int i = 0; i < authMethods.size() - 1; i++) {
            assertThat(authMethods.get(i), not(IsInstanceOf.instanceOf(AuthPassword.class)));
        }
    }
}