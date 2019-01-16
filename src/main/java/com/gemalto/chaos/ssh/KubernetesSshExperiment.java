package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.impl.KubernetesSshManager;
import com.google.common.io.ByteStreams;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class KubernetesSshExperiment extends SshExperiment {
    private Exec exec;

    private KubernetesSshExperiment (String experimentName, String experimentScript) {
        super(experimentName, experimentScript);
        buildRequiredCapabilities();
        this.exec = new Exec();
    }

    @Override
    protected void buildRequiredCapabilities () {
    }

    @Override
    public void runExperiment () throws ChaosException {
        KubernetesSshManager sshManager = getSshManager();
        try {
            String script = StreamUtils.copyToString(shResourceService.getScriptResource(getExperimentScript())
                                                                      .getInputStream(), Charset.defaultCharset());
            final Process proc = exec.exec(sshManager.getNamespace(), sshManager.getPodname(), new String[]{ "nohup", "sh", "-c", script, "&" }, false, false);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteStreams.copy(proc.getInputStream(), byteArrayOutputStream);
            log.info("Got response {} from Kubernetes Pod", byteArrayOutputStream.toString());
            proc.destroy();
        } catch (ApiException | IOException e) {
            log.error("Cannot execute SSH experiment {}.", getExperimentName());
            throw new ChaosException("Cannot execute SSH experiment.", e);
        }
    }

    public KubernetesSshManager getSshManager () {
        return (KubernetesSshManager) this.sshManager;
    }

    public static KubernetesSshExperiment fromExperiment (SshExperiment experiment) {
        return new KubernetesSshExperiment(experiment.getExperimentName(), experiment.getExperimentScript());
    }

    public KubernetesSshExperiment setExec (Exec exec) {
        this.exec = exec;
        return this;
    }
}
