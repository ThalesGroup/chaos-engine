package com.gemalto.chaos.shellclient.impl;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.constants.SSHConstants;
import com.gemalto.chaos.shellclient.ShellClient;
import com.gemalto.chaos.shellclient.ShellConstants;
import com.gemalto.chaos.util.ShellUtils;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static net.logstash.logback.argument.StructuredArguments.v;

public class KubernetesShellClient implements ShellClient {
    private static final Logger log = LoggerFactory.getLogger(KubernetesShellClient.class);
    private Exec exec;
    private String namespace;
    private String podName;
    private String containerName;

    public static KubernetesShellClientBuilder builder () {
        return KubernetesShellClientBuilder.aKubernetesShellClient();
    }

    @Override
    public String runCommand (String command) {
        return runCommand(command, true);
    }

    String runCommand (String command, boolean getOutput) {
        log.debug("Running command {}, waiting for output = {}", v("command", command), getOutput);
        Process proc = null;
        try {
            proc = exec.exec(namespace, podName, command.split(" "), containerName, false, false);
            if (getOutput) {
                proc.waitFor();
                return StreamUtils.copyToString(proc.getInputStream(), Charset.defaultCharset());
            } else {
                return Strings.EMPTY;
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while processing, throwing Interrupt up thread", e);
            Thread.currentThread().interrupt();
            return Strings.EMPTY;
        } catch (IOException | ApiException e) {
            log.error("Received exception while processing command", e);
            throw new ChaosException(e);
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    @Override
    public String runResource (Resource resource) {
        try {
            return runCommand(String.format(SSHConstants.SCRIPT_NOHUP_WRAPPER, copyResourceToPath(resource)), false);
        } catch (IOException e) {
            throw new ChaosException(e);
        }
    }

    String copyResourceToPath (Resource resource) throws IOException {
        return copyResourceToPath(resource, SSHConstants.TEMP_DIRECTORY);
    }

    String copyResourceToPath (Resource resource, String path) throws IOException {
        log.debug("Transferring {} to {}/{} in path {}", resource, namespace, podName, path);
        String finalPath = path + (path.endsWith("/") ? Strings.EMPTY : "/") + resource.getFilename();
        String[] command = new String[]{ "tar", "xf", "-", "-C", path };
        Process proc = null;
        try {
            proc = exec.exec(namespace, podName, command, containerName, true, false);
            try (OutputStream os = proc.getOutputStream()) {
                try (TarArchiveOutputStream tarOS = new TarArchiveOutputStream(os)) {
                    TarArchiveEntry archiveEntry = new TarArchiveEntry(resource.getFile(), resource.getFilename());
                    archiveEntry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE | ShellConstants.EXEC_BIT);
                    tarOS.putArchiveEntry(archiveEntry);
                    try (InputStream inputStream = resource.getInputStream()) {
                        StreamUtils.copy(inputStream, tarOS);
                    }
                    tarOS.closeArchiveEntry();
                    os.flush();
                    tarOS.flush();
                    os.write(ShellConstants.EOT_CHARACTER);
                }
            }
            proc.destroy();
            proc.waitFor();
            int i = proc.exitValue();
            if (ShellUtils.isTarSuccessful(i)) {
                proc = null;
                return finalPath;
            }
            throw new ChaosException("Received exit code " + i + " when transferring file");
        } catch (ApiException e) {
            log.error("Received exception while processing command", e);
            throw new ChaosException(e);
        } catch (InterruptedException e) {
            log.error("Interrupted while processing, throwing Interrupt up thread", e);
            Thread.currentThread().interrupt();
            return Strings.EMPTY;
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    @Override
    public void close () {
        /*
        This does not maintain any persistent channels, so does not need to actually close, unlike most implementations
        of a shell client.
         */
    }

    public static final class KubernetesShellClientBuilder {
        private Exec exec;
        private String namespace;
        private String podName;
        private String containerName;

        private KubernetesShellClientBuilder () {
        }

        public static KubernetesShellClientBuilder aKubernetesShellClient () {
            return new KubernetesShellClientBuilder();
        }

        public KubernetesShellClientBuilder withExec (Exec exec) {
            this.exec = exec;
            return this;
        }

        public KubernetesShellClientBuilder withNamespace (String namespace) {
            this.namespace = namespace;
            return this;
        }

        public KubernetesShellClientBuilder withPodName (String podName) {
            this.podName = podName;
            return this;
        }

        public KubernetesShellClientBuilder withContainerName (String containerName) {
            this.containerName = containerName;
            return this;
        }

        public KubernetesShellClient build () {
            KubernetesShellClient kubernetesShellClient = new KubernetesShellClient();
            kubernetesShellClient.exec = this.exec;
            kubernetesShellClient.podName = this.podName;
            kubernetesShellClient.namespace = this.namespace;
            kubernetesShellClient.containerName = this.containerName;
            return kubernetesShellClient;
        }
    }
}
