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
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ChaosSSHClientTest {
    private ChaosSSHClient chaosSSHClient = Mockito.spy(new ChaosSSHClient());

    @Test(expected = ChaosException.class)
    public void getSshClientException () {
        chaosSSHClient.getSshClient();
    }

    @Test
    public void connect () throws IOException {
        String hostname = randomUUID().toString();
        int port = new Random().nextInt(65534) + 1;
        SSHCredentials sshCredentials = Mockito.spy(SSHCredentials.class);
        SSHClient sshClient = Mockito.mock(SSHClient.class);
        String username = randomUUID().toString();
        Collection<AuthMethod> authMethodCollection = Collections.emptyList();
        doReturn(username).when(sshCredentials).getUsername();
        doReturn(authMethodCollection).when(sshCredentials).getAuthMethods();
        //
        //
        // Test all the Null Pointer Exceptions for required objects.
        try {
            chaosSSHClient.connect();
            fail("Expected NPE");
        } catch (NullPointerException ignored) {
        }
        try {
            chaosSSHClient.withEndpoint(hostname, null).connect();
            fail("Expected NPE");
        } catch (NullPointerException ignored) {
        }
        try {
            chaosSSHClient.withEndpoint(hostname, port).connect();
            fail("Expected NPE");
        } catch (NullPointerException ignored) {
        }
        //
        //
        // Test a successful connection
        try {
            doReturn(sshClient).when(chaosSSHClient).buildNewSSHClient();
            doNothing().when(sshClient).connect(hostname, port);
            doNothing().when(sshClient).auth(username, authMethodCollection);
            chaosSSHClient.withSSHCredentials(sshCredentials).connect();
            verify(sshClient, times(1)).connect(hostname, port);
            verify(sshClient, times(1)).auth(username, authMethodCollection);
            verify(sshClient, never()).close();
        } catch (NullPointerException e) {
            fail("Should not have an NPE now");
        }
        //
        //
        // Test an IO Exception on the connection attempt
        reset(sshClient);
        doThrow(new IOException()).when(sshClient).connect(hostname, port);
        try {
            chaosSSHClient.connect();
            fail("Expected a thrown ExecutionException to produce a ChaosException");
        } catch (ChaosException e) {
            assertEquals(e.getCause().getClass(), ExecutionException.class);
        }
        //
        //
        // Test a Transport exception
        reset(sshClient);
        doNothing().when(sshClient).connect(hostname, port);
        doThrow(new TransportException(Strings.EMPTY)).when(sshClient).auth(username, authMethodCollection);
        try {
            chaosSSHClient.connect();
            fail("Expected a thrown TransportException to produce a ChaosException");
        } catch (ChaosException e) {
            assertEquals(e.getCause().getClass(), TransportException.class);
        }
        verify(chaosSSHClient, times(1)).close();
        //
        //
        // Test an Authentication exception
        reset(sshClient);
        doNothing().when(sshClient).connect(hostname, port);
        doThrow(new UserAuthException(Strings.EMPTY)).when(sshClient).auth(username, authMethodCollection);
        try {
            chaosSSHClient.connect();
            fail("Expected a thrown UserAuthException to produce a ChaosException");
        } catch (ChaosException e) {
            assertEquals(e.getCause().getClass(), UserAuthException.class);
        }
        verify(chaosSSHClient, times(2)).close();
    }

    @Test
    public void withSSHCredentials () {
        assertSame(chaosSSHClient, chaosSSHClient.withSSHCredentials(null));
    }

    @Test
    public void withEndpoint () {
        assertSame(chaosSSHClient, chaosSSHClient.withEndpoint(randomUUID().toString()));
        assertSame(chaosSSHClient, chaosSSHClient.withEndpoint(randomUUID().toString(), 1));
    }

    @Test
    public void withEndpointInvalidPorts () {
        int[] badPorts = { -1000, -65535, -65536, -1, 0, 65536, 100000 };
        for (int badPort : badPorts) {
            try {
                chaosSSHClient.withEndpoint(null, badPort);
                fail("Should have thrown an exception");
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Test
    public void runCommand () throws Exception {
        String shellCommand = randomUUID().toString();
        String output = randomUUID().toString();
        SSHClient sshClient = Mockito.mock(SSHClient.class, RETURNS_DEEP_STUBS);
        Session session = sshClient.startSession();
        Session.Command command = session.exec(shellCommand);
        doReturn(new ByteArrayInputStream(output.getBytes())).when(command).getInputStream();
        doReturn(sshClient).when(chaosSSHClient).getSshClient();
        assertEquals(output, chaosSSHClient.runCommand(shellCommand).getStdOut());
    }

    @Test(timeout = 30000L)
    public void runResource () throws Exception {
        Resource resourceFile = Mockito.mock(Resource.class, RETURNS_DEEP_STUBS);
        String SCRIPT_NAME = randomUUID().toString();
        String output = randomUUID().toString();
        doReturn(SCRIPT_NAME).when(resourceFile).getFilename();
        SSHClient sshClient = Mockito.mock(SSHClient.class, RETURNS_DEEP_STUBS);
        Session session = sshClient.startSession();
        SCPFileTransfer scpFileTransfer = sshClient.newSCPFileTransfer();
        String command = "/tmp/" + SCRIPT_NAME;
        doReturn(output).when(chaosSSHClient).runCommandInShell(session, command);
        doReturn(sshClient).when(chaosSSHClient).getSshClient();
        assertEquals(output, chaosSSHClient.runResource(resourceFile));
    }

    @Test
    public void close () throws Exception {
        SSHClient mockClient = Mockito.mock(SSHClient.class);
        doReturn(mockClient).when(chaosSSHClient).getSshClient();
        chaosSSHClient.close();
        verify(mockClient, times(1)).close();
    }
}