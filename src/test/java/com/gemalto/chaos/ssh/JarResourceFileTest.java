package com.gemalto.chaos.ssh;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JarResourceFileTest {
    @Mock
    Resource resource;

    @Mock
    InputStream stream;

    @Before
    public void setUp () throws Exception {
        when(resource.getFilename()).thenReturn("");
        when(resource.getInputStream()).thenReturn(stream);
    }

    @Test
    public void getPermissions() throws IOException {
        JarResourceFile file= new JarResourceFile(resource,true);
        assertEquals(0744,file.getPermissions());

        file= new JarResourceFile(resource,false);
        assertEquals(0644,file.getPermissions());
    }

    @Test
    public void getLength() throws IOException {
        JarResourceFile file= new JarResourceFile(resource,true);
        when(resource.contentLength()).thenReturn(new Long(1));
        assertEquals(1,file.getLength());
        when(resource.contentLength()).thenThrow(IOException.class);
        assertEquals(0,file.getLength());
    }

    @Test
    public void isFile(){
        JarResourceFile file= new JarResourceFile(resource,true);
        assertTrue(file.isFile());
    }

    @Test
    public void modification() throws IOException {
        JarResourceFile file= new JarResourceFile(resource,true);
        assertEquals(file.getLastAccessTime(),file.getLastModifiedTime());
    }

    @Test
    public void getInputStream() throws IOException {
        JarResourceFile file= new JarResourceFile(resource,true);
        assertEquals(stream,file.getInputStream());
    }
}