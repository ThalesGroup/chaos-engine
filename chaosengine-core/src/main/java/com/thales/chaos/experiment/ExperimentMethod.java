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

package com.thales.chaos.experiment;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.shellclient.ShellOutput;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import static com.thales.chaos.exception.enums.ChaosErrorCode.ERROR_CREATING_EXPERIMENT_METHOD_FROM_JAVA;
import static com.thales.chaos.exception.enums.ChaosErrorCode.PLATFORM_DOES_NOT_SUPPORT_SHELL_EXPERIMENTS;

public class ExperimentMethod<T extends Container> implements BiConsumer<T, Experiment> {
    private BiConsumer<T, Experiment> actualBiconsumer;
    private String experimentName;
    private boolean cattleOnly;
    private ExperimentType experimentType;

    @SuppressWarnings("unchecked")
    static ExperimentMethod fromMethod (final Method method) {
        try {
            Class<? extends Container> declaringClass = (Class<? extends Container>) method.getDeclaringClass();
        } catch (ClassCastException e) {
            throw new ChaosException(ERROR_CREATING_EXPERIMENT_METHOD_FROM_JAVA, e);
        }
        final ChaosExperiment experimentConfiguration = AnnotationUtils.getAnnotation(method, ChaosExperiment.class);
        final ExperimentMethod experimentMethod = new ExperimentMethod();
        experimentMethod.actualBiconsumer = (container, experiment) -> {
            try {
                ((Experiment) experiment).setMinimumDuration(Duration.ofSeconds(experimentConfiguration.minimumDurationInSeconds()));
                ((Experiment) experiment).setMaximumDuration(Duration.ofSeconds(experimentConfiguration.maximumDurationInSeconds()));
                method.invoke(container, experiment);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ChaosException(ERROR_CREATING_EXPERIMENT_METHOD_FROM_JAVA, e);
            }
        };
        experimentMethod.experimentName = method.getName();
        experimentMethod.experimentType = experimentConfiguration.experimentType();
        experimentMethod.cattleOnly = experimentConfiguration.cattleOnly();
        return experimentMethod;
    }

    static <T extends Container> ExperimentMethod fromScript (T container, Script script) {
        if (!container.supportsShellBasedExperiments()) {
            throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL_EXPERIMENTS);
        }
        final boolean cattle = script.isRequiresCattle();
        final Runnable selfHealingMethod = cattle ? container.recycleCattle() : () -> container.runCommand(script.getSelfHealingCommand());
        Callable<ContainerHealth> checkContainerHealthMethod = () -> {
            if (cattle) {
                return container.isContainerRecycled() ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
            }
            try {
                ShellOutput shellOutput = container.runCommand(script.getHealthCheckCommand());
                return shellOutput.getExitCode() == 0 ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT;
            } catch (RuntimeException e) {
                return ContainerHealth.RUNNING_EXPERIMENT;
            }
        };
        Runnable finalizeMethod;
        String finalizeCommand = script.getFinalizeCommand();
        finalizeMethod = (finalizeCommand == null || finalizeCommand.isBlank()) ? null : () -> container.runCommand(finalizeCommand);
        final ExperimentMethod experimentMethod = new ExperimentMethod();
        experimentMethod.experimentType = script.getExperimentType();
        experimentMethod.cattleOnly = cattle;
        experimentMethod.experimentName = script.getScriptName();
        experimentMethod.actualBiconsumer = (BiConsumer<Container, Experiment>) (container1, experiment) -> {
            experiment.setMaximumDuration(script.getMaximumDuration());
            experiment.setMinimumDuration(script.getMinimumDuration());
            experiment.setSelfHealingMethod(selfHealingMethod);
            experiment.setCheckContainerHealth(checkContainerHealthMethod);
            experiment.setFinalizeMethod(finalizeMethod);
            container1.runScript(script);
        };
        return experimentMethod;
    }

    @Override
    public void accept (T container, Experiment experiment) {
        actualBiconsumer.accept(container, experiment);
    }

    public String getExperimentName () {
        return experimentName;
    }

    public boolean isCattleOnly () {
        return cattleOnly;
    }

    public ExperimentType getExperimentType () {
        return experimentType;
    }
}
