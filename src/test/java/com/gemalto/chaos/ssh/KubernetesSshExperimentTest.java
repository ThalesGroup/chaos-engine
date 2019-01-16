package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.impl.KubernetesSshManager;
import com.gemalto.chaos.ssh.services.ShResourceService;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesSshExperimentTest {
    private static final String experimentName = "Generic SSH experiment";
    private static final String experimentScript = UUID.randomUUID().toString();
    @Mock
    private static ApiClient apiClient;
    @Mock
    private KubernetesSshManager sshManager;
    @Mock
    private Resource resource;
    @Mock
    private ShResourceService shResourceService;
    @Mock
    private Exec exec;
    @Mock
    private InputStream mockInputStream;
    private KubernetesSshExperiment genericSshExperiment = KubernetesSshExperiment.fromExperiment(new GenericSshExperiment(experimentName, experimentScript));

    @Before
    public void setUp () throws Exception {
        when(shResourceService.getScriptResource(experimentScript)).thenReturn(resource);
        when(sshManager.getPodname()).thenReturn("podname");
        when(sshManager.getNamespace()).thenReturn("namespace");
        genericSshExperiment.setExec(exec).setSshManager(sshManager).setShResourceService(shResourceService);
        when(mockInputStream.read(any(byte[].class))).thenThrow(new IOException());
    }

    @Test
    public void runExperiment () throws Exception {
        when(exec.exec(anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenReturn(new TestProcess(new ByteArrayInputStream("test data"
                .getBytes())));
        genericSshExperiment.runExperiment();
    }

    @Test(expected = ChaosException.class)
    public void runExperimentWithIOException () throws IOException, ApiException {
        when(exec.exec(anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenReturn(new TestProcess(mockInputStream));
        genericSshExperiment.runExperiment();
    }

    @Test(expected = ChaosException.class)
    public void runExperimentWithApiException () throws IOException, ApiException {
        when(exec.exec(anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenThrow(new ApiException());
        genericSshExperiment.runExperiment();
    }

    class GenericSshExperiment extends SshExperiment {
        public GenericSshExperiment (String experimentName, String experimentScript) {
            super(experimentName, experimentScript);
        }

        @Override
        protected void buildRequiredCapabilities () {
        }
    }

    private class TestProcess extends Process {
        private InputStream is;

        public TestProcess (InputStream is) {
            this.is = is;
        }

        @Override
        public OutputStream getOutputStream () {
            return null;
        }

        @Override
        public InputStream getInputStream () {
            return is;
        }

        @Override
        public InputStream getErrorStream () {
            return null;
        }

        @Override
        public int waitFor () throws InterruptedException {
            return 0;
        }

        @Override
        public int exitValue () {
            return 0;
        }

        @Override
        public void destroy () {
        }
    }
}