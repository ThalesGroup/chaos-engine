package com.thales.chaos.experiment;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.annotations.CattleExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.shellclient.ShellOutput;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
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
        final ExperimentMethod experimentMethod = new ExperimentMethod();
        experimentMethod.actualBiconsumer = (container, experiment) -> {
            try {
                method.invoke(container, experiment);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ChaosException(ERROR_CREATING_EXPERIMENT_METHOD_FROM_JAVA, e);
            }
        };
        experimentMethod.experimentName = method.getName();
        Optional<Annotation> experimentTypeAnnotation = Arrays.stream(method.getAnnotations())
                                                              .filter(ExperimentType::isExperiment)
                                                              .findFirst();
        experimentTypeAnnotation.ifPresent(annotation -> experimentMethod.experimentType = ExperimentType.valueOf(annotation));
        experimentMethod.cattleOnly = Arrays.stream(method.getAnnotations())
                                            .map(Annotation::annotationType)
                                            .anyMatch(c -> c.equals(CattleExperiment.class));
        return experimentMethod;
    }

    static <T extends Container> ExperimentMethod fromScript (T container, Script script) {
        if (!container.supportsShellBasedExperiments()) {
            throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL_EXPERIMENTS);
        }
        final boolean cattle = script.isRequiresCattle();
        final Callable<Void> selfHealingMethod = cattle ? container.recycleCattle() : () -> {
            container.runCommand(script.getSelfHealingCommand());
            return null;
        };
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
        Callable<Void> finalizeMethod = () -> {
            container.runCommand(script.getFinalizeCommand());
            return null;
        };
        final ExperimentMethod experimentMethod = new ExperimentMethod();
        experimentMethod.experimentType = script.getExperimentType();
        experimentMethod.cattleOnly = cattle;
        experimentMethod.experimentName = script.getScriptName();
        experimentMethod.actualBiconsumer = (BiConsumer<Container, Experiment>) (container1, experiment) -> {
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
