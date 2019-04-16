package com.gemalto.chaos.shellclient.impl;

import com.gemalto.chaos.constants.SSHConstants;
import com.gemalto.chaos.exception.ChaosException;
import com.gemalto.chaos.shellclient.ShellOutput;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URI;
import java.net.URL;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesShellClientTest {
    private static final String CONTAINER_NAME = randomUUID().toString();
    private static final String POD_NAME = randomUUID().toString();
    private static final String NAMESPACE = randomUUID().toString();
    private KubernetesShellClient kubernetesShellClient;
    @Mock
    private Exec exec;

    @Before
    public void setUp () {
        kubernetesShellClient = Mockito.spy(KubernetesShellClient.builder()
                                                                 .withExec(exec)
                                                                 .withNamespace(NAMESPACE)
                                                                 .withPodName(POD_NAME)
                                                                 .withContainerName(CONTAINER_NAME)
                                                                 .build());
    }

    @Test
    public void runCommandSimple () {
        String command = randomUUID().toString();
        String output = randomUUID().toString();
        doReturn(new ShellOutput(0, output)).when(kubernetesShellClient).runCommand(command, true);
        assertEquals(output, kubernetesShellClient.runCommand(command).getStdOut());
        verify(kubernetesShellClient, times(1)).runCommand(command, true);
    }

    @Test
    public void runCommand () throws Exception {
        String expectedOutput = randomUUID().toString();
        Process process = mock(Process.class);
        String command = randomUUID().toString();
        doReturn(process).when(exec)
                         .exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        doReturn(0).when(process).waitFor();
        doReturn(new ByteArrayInputStream(expectedOutput.getBytes())).when(process).getInputStream();
        String output = kubernetesShellClient.runCommand(command, true).getStdOut();
        assertEquals(expectedOutput, output);
        verify(exec, times(1)).exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        verify(process, times(1)).destroy();
    }

    @Test
    public void runCommandInterruptException () throws Exception {
        String expectedOutput = Strings.EMPTY;
        Process process = mock(Process.class);
        String command = randomUUID().toString();
        Throwable exception = new InterruptedException();
        doReturn(process).when(exec)
                         .exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        doThrow(exception).when(process).waitFor();
        String output = kubernetesShellClient.runCommand(command, true).getStdOut();
        assertEquals(expectedOutput, output);
        verify(exec, times(1)).exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        verify(process, times(1)).destroy();
        assertTrue(Thread.interrupted());
    }

    @Test
    public void runCommandAPIException () throws Exception {
        Process process = mock(Process.class);
        String command = randomUUID().toString();
        Throwable exception = new ApiException();
        doThrow(exception).when(exec)
                          .exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        try {
            kubernetesShellClient.runCommand(command, true);
            fail("Expected an API exception");
        } catch (ChaosException e) {
            assertEquals(exception, e.getCause());
        }
        verify(process, never()).waitFor();
        verify(exec, times(1)).exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        verify(process, never()).destroy();
    }

    @Test
    public void runCommandIOException () throws Exception {
        Process process = mock(Process.class);
        String command = randomUUID().toString();
        Throwable exception = new IOException();
        doThrow(exception).when(exec)
                          .exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        try {
            kubernetesShellClient.runCommand(command, true);
            fail("Expected an API exception");
        } catch (ChaosException e) {
            assertEquals(exception, e.getCause());
        }
        verify(process, never()).waitFor();
        verify(exec, times(1)).exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        verify(process, never()).destroy();
    }

    @Test
    public void runCommandNoOutput () throws Exception {
        String expectedOutput = "";
        Process process = mock(Process.class);
        String command = randomUUID().toString();
        doReturn(process).when(exec)
                         .exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        String output = kubernetesShellClient.runCommand(command, false).getStdOut();
        assertEquals(expectedOutput, output);
        verify(exec, times(1)).exec(NAMESPACE, POD_NAME, new String[]{ "sh", "-c", command }, CONTAINER_NAME, false, false);
        verify(process, times(1)).waitFor();
        verify(process, never()).getInputStream();
        verify(process, times(1)).destroy();
    }

    @Test
    public void runResource () throws IOException {
        Resource resource = mock(Resource.class);
        String finalPath = randomUUID().toString();
        String command = String.format(SSHConstants.SCRIPT_NOHUP_WRAPPER, finalPath);
        doReturn(finalPath).when(kubernetesShellClient).copyResourceToPath(resource, "/tmp/");
        doReturn(ShellOutput.EMPTY_SHELL_OUTPUT).when(kubernetesShellClient)
                                                .runCommand(new String[]{ "sh", "-c", command }, false);
        kubernetesShellClient.runResource(resource);
        verify(kubernetesShellClient, times(1)).copyResourceToPath(resource, "/tmp/");
        verify(kubernetesShellClient, times(1)).runCommand(new String[]{ "sh", "-c", command }, false);
    }

    @Test
    public void runResourceException () throws IOException {
        Resource resource = mock(Resource.class);
        IOException exception = new IOException();
        doThrow(exception).when(kubernetesShellClient).copyResourceToPath(resource, "/tmp/");
        try {
            kubernetesShellClient.runResource(resource);
            fail("Expected to get a ChaosException");
        } catch (ChaosException e) {
            assertEquals(exception, e.getCause());
        }
    }

    @Test
    public void copyResourceToPathWrapper () throws IOException {
        Resource resource = mock(Resource.class);
        String finalPath = randomUUID().toString();
        doReturn(finalPath).when(kubernetesShellClient).copyResourceToPath(resource, "/tmp/");
        assertEquals(finalPath, kubernetesShellClient.copyResourceToPath(resource));
        verify(kubernetesShellClient, times(1)).copyResourceToPath(resource, "/tmp/");
    }

    @Test
    public void copyResourceToPath () throws Exception {
        String filename = randomUUID().toString();
        Resource resource = testResource(filename);
        String finalPath = "/tmp/" + filename;
        Process process = mock(Process.class);
        OutputStream outputStream = mock(OutputStream.class);
        doReturn(process).when(exec)
                         .exec(NAMESPACE, POD_NAME, ("dd if=/dev/stdin of=/tmp/" + filename + " bs=" + resource.contentLength() + " count=1")
                                 .split(" "), CONTAINER_NAME, true, false);
        doReturn(outputStream).when(process).getOutputStream();
        doReturn(true).when(process).waitFor(anyLong(), any());
        doReturn(new ShellOutput(0, "OK")).when(kubernetesShellClient).runCommand(any());
        assertEquals(finalPath, kubernetesShellClient.copyResourceToPath(resource, "/tmp/"));
        verify(outputStream, atLeastOnce()).flush();
        verify(process, times(1)).destroy();
        verify(process, times(1)).waitFor(anyLong(), any());
        verify(process, times(1)).exitValue();
    }

    private static Resource testResource (String filename) {
        return new Resource() {
            @Override
            public boolean exists () {
                return true;
            }

            @Override
            public URL getURL () {
                return null;
            }

            @Override
            public URI getURI () {
                return null;
            }

            @Override
            public File getFile () throws IOException {
                return File.createTempFile(randomUUID().toString(), randomUUID().toString());
            }

            @Override
            public long contentLength () throws IOException {
                return getFile().length();
            }

            @Override
            public long lastModified () {
                return 0;
            }

            @Override
            public Resource createRelative (String s) {
                return null;
            }

            @Override
            public String getFilename () {
                return filename;
            }

            @Override
            public String getDescription () {
                return null;
            }

            @Override
            public InputStream getInputStream () throws IOException {
                return new FileInputStream(getFile());
            }
        };
    }

    @Test
    public void copyResourceToPathApiException () throws Exception {
        String filename = randomUUID().toString();
        Resource resource = testResource(filename);
        long contentLength = resource.contentLength();

        Throwable exception = new ApiException();
        doThrow(exception).when(exec)
                          .exec(NAMESPACE, POD_NAME, ("dd if=/dev/stdin of=/tmp/" + filename + " bs=" + contentLength + " count=1")
                                  .split(" "), CONTAINER_NAME, true, false);
        try {
            kubernetesShellClient.copyResourceToPath(resource, "/tmp/");
            fail("Expected an exception");
        } catch (ChaosException e) {
            assertEquals(exception, e.getCause());
        }
    }

    @Test
    public void copyResourceToPathInterruptedException () throws Exception {
        Throwable exception = new InterruptedException();
        String filename = randomUUID().toString();
        Resource resource = testResource(filename);
        Process process = mock(Process.class);
        OutputStream outputStream = mock(OutputStream.class);
        doReturn(process).when(exec)
                         .exec(NAMESPACE, POD_NAME, ("dd if=/dev/stdin of=/tmp/" + filename + " bs=" + resource.contentLength() + " count=1")
                                 .split(" "), CONTAINER_NAME, true, false);
        doReturn(outputStream).when(process).getOutputStream();
        doThrow(exception).when(process).waitFor(anyLong(), any());
        assertEquals("", kubernetesShellClient.copyResourceToPath(resource, "/tmp/"));
        verify(process, atLeastOnce()).destroy();
        verify(process, never()).exitValue();
        assertTrue(Thread.interrupted());
    }
}