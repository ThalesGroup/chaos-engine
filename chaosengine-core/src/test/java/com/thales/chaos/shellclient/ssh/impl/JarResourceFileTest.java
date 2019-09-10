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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class JarResourceFileTest {
    @Mock
    private Resource resource;
    @Mock
    private InputStream inputStream;
    private JarResourceFile jarResourceFile;
    private String filename = UUID.randomUUID().toString();

    @Before
    public void setUp () throws Exception {
        doReturn(filename).when(resource).getFilename();
        doReturn(inputStream).when(resource).getInputStream();
        jarResourceFile = new JarResourceFile(resource, true);
    }

    @Test
    public void isFile () {
        assertTrue(jarResourceFile.isFile());
    }

    @Test
    public void getLength () throws Exception {
        long contentLength;
        doReturn((contentLength = new Random().nextLong())).when(resource).contentLength();
        assertEquals(contentLength, jarResourceFile.getLength());
    }

    @Test
    public void getLengthIOException () throws Exception {
        doThrow(new IOException()).when(resource).contentLength();
        assertEquals(0, jarResourceFile.getLength());
    }

    @Test
    public void getInputStream () throws Exception {
        assertSame(inputStream, jarResourceFile.getInputStream());
    }

    @Test
    public void getLastAccessTime () {
        long before = System.currentTimeMillis() / 1000;
        long actual = jarResourceFile.getLastAccessTime();
        long after = System.currentTimeMillis() / 1000;
        assertFalse(before > actual);
        assertFalse(after < actual);
    }

    @Test
    public void getLastModifiedTime () {
        long before = System.currentTimeMillis() / 1000;
        long actual = jarResourceFile.getLastModifiedTime();
        long after = System.currentTimeMillis() / 1000;
        assertFalse(before > actual);
        assertFalse(after < actual);
    }

    @Test
    public void getPermissions () {
        assertEquals(0744, jarResourceFile.getPermissions());
        JarResourceFile nonExecutable = new JarResourceFile(resource, false);
        assertEquals(0644, nonExecutable.getPermissions());
    }
}