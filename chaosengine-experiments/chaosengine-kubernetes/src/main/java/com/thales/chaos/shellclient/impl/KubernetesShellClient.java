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

package com.thales.chaos.shellclient.impl;

import com.thales.chaos.constants.SSHConstants;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.KubernetesChaosErrorCode;
import com.thales.chaos.shellclient.ShellClient;
import com.thales.chaos.shellclient.ShellOutput;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.StringJoiner;
import java.util.concurrent.*;

import static net.logstash.logback.argument.StructuredArguments.v;

public class KubernetesShellClient implements ShellClient {
    private static final Logger log = LoggerFactory.getLogger(KubernetesShellClient.class);
    private ApiClient apiClient;
    private String namespace;
    private String podName;
    private String containerName;

    public static KubernetesShellClientBuilder builder () {
        return KubernetesShellClientBuilder.aKubernetesShellClient();
    }

    @Override
    public String runResource (Resource resource) {
        try {
            String innerCommand = String.format(SSHConstants.SCRIPT_NOHUP_WRAPPER, copyResourceToPath(resource));
            return runCommand(new String[]{ "sh", "-c", innerCommand }, false).getStdOut();
        } catch (IOException e) {
            throw new ChaosException(KubernetesChaosErrorCode.K8S_SHELL_CLIENT_ERROR, e);
        }
    }

    ShellOutput runCommand (String[] command, boolean getOutput) {
        log.debug("Running command {}, waiting for output = {}", v("command", command), getOutput);
        Process proc = null;
        try {
            proc = getExec().exec(namespace, podName, command, containerName, false, false);
            if (getOutput) {
                Future<String> output = Executors.newSingleThreadExecutor()
                                                 .submit(getFutureOutputFromInputStream(proc.getInputStream()));
                int exitCode = proc.waitFor();
                proc.destroy();
                proc = null;
                String stdOut = getOutputFromFutureString(output);
                ShellOutput shellOutput = ShellOutput.builder().withExitCode(exitCode).withStdOut(stdOut).build();
                if (exitCode > 0) {
                    log.debug("Command execution failed {}", v("failure", shellOutput));
                }
                return shellOutput;
            } else {
                proc.waitFor();
                return ShellOutput.EMPTY_SHELL_OUTPUT;
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while processing, throwing Interrupt up thread", e);
            Thread.currentThread().interrupt();
            return ShellOutput.EMPTY_SHELL_OUTPUT;
        } catch (IOException | ApiException e) {
            throw new ChaosException(KubernetesChaosErrorCode.K8S_SHELL_CLIENT_ERROR, e);
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    Exec getExec () {
        return new Exec(apiClient);
    }

    private static Callable<String> getFutureOutputFromInputStream (InputStream in) {
        return () -> {
            StringJoiner outputBuilder = new StringJoiner("\n");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    outputBuilder.add(line);
                }
            }
            return outputBuilder.toString();
        };
    }

    private static <T> T getOutputFromFutureString (Future<T> futureMethod) throws IOException, InterruptedException {
        try {
            return futureMethod.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    String copyResourceToPath (Resource resource, String path) throws IOException {
        long contentLength = resource.contentLength();
        String finalPath = path + (path.endsWith("/") ? Strings.EMPTY : "/") + resource.getFilename();
        String[] command = new String[]{ "dd", "if=/dev/stdin", "of=" + finalPath, "bs=" + contentLength, "count=1" };
        log.debug("Transferring {} to {}/{} in path {}", resource, namespace, podName, path);
        Process proc = null;
        try {
            proc = getExec().exec(namespace, podName, command, containerName, true, false);
            try (OutputStream os = proc.getOutputStream()) {
                try (InputStream is = resource.getInputStream()) {
                    StreamUtils.copy(is, os);
                }
            }
            if (!proc.waitFor(5000, TimeUnit.MILLISECONDS)) {
                throw new ChaosException(KubernetesChaosErrorCode.K8S_SHELL_TRANSFER_TIMEOUT);
            }
            int i = proc.exitValue();
            if (i == 0) {
                String command1 = "chmod 755 " + finalPath;
                runCommand(command1);
                return finalPath;
            }
            throw new ChaosException(KubernetesChaosErrorCode.K8S_SHELL_TRANSFER_FAIL);
        } catch (ApiException e) {
            throw new ChaosException(KubernetesChaosErrorCode.K8S_SHELL_TRANSFER_FAIL, e);
        } catch (InterruptedException e) {
            log.error("Interrupted while processing, throwing Interrupt up thread", e);
            Thread.currentThread().interrupt();
            return Strings.EMPTY;
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    ShellOutput runCommand (String command, boolean getOutput) {
        return runCommand(new String[]{ "sh", "-c", command }, getOutput);
    }

    String copyResourceToPath (Resource resource) throws IOException {
        return copyResourceToPath(resource, SSHConstants.TEMP_DIRECTORY);
    }

    @Override
    public ShellOutput runCommand (String command) {
        return runCommand(command, true);
    }

    @Override
    public void close () {
        /*
        This does not maintain any persistent channels, so does not need to actually close, unlike most implementations
        of a shell client.
         */
    }

    public static final class KubernetesShellClientBuilder {
        private ApiClient apiClient;
        private String namespace;
        private String podName;
        private String containerName;

        private KubernetesShellClientBuilder () {
        }

        static KubernetesShellClientBuilder aKubernetesShellClient () {
            return new KubernetesShellClientBuilder();
        }

        public KubernetesShellClientBuilder withApiClient (ApiClient apiClient) {
            this.apiClient = apiClient;
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
            kubernetesShellClient.apiClient = this.apiClient;
            kubernetesShellClient.podName = this.podName;
            kubernetesShellClient.namespace = this.namespace;
            kubernetesShellClient.containerName = this.containerName;
            return kubernetesShellClient;
        }
    }
}
