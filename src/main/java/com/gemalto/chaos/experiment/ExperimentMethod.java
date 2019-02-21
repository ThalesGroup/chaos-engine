package com.gemalto.chaos.experiment;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.annotations.CattleExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.scripts.Script;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

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
            throw new ChaosException("Trying to create an experiment method from a non-container method", e);
        }
        final ExperimentMethod experimentMethod = new ExperimentMethod();
        experimentMethod.actualBiconsumer = (container, experiment) -> {
            try {
                method.invoke(container, experiment);
            } catch (IllegalAccessException e) {
                throw new ChaosException("Tried to start an experiment via reflection without proper method permissions", e);
            } catch (InvocationTargetException e) {
                throw new ChaosException("Issue starting an experiment via reflection", e);
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
            throw new ChaosException("Cannot create script-based experiment for a container that doesn't support script-based experiments");
        }
        final boolean cattle = script.isRequiresCattle();
        final Callable<Void> selfHealingMethod = cattle ? container.recycleCattle() : () -> {
            container.runCommand(script.getSelfHealingCommand());
            return null;
        };
        Callable<ContainerHealth> checkContainerHealthMethod = () -> {
            if (cattle && container.isContainerRecycled()) return ContainerHealth.NORMAL;
            try {
                container.runCommand(script.getSelfHealingCommand());
                return ContainerHealth.NORMAL;
            } catch (RuntimeException e) {
                return ContainerHealth.RUNNING_EXPERIMENT;
            }
        };
        Callable<Void> finalizeMethod = () -> {
            script.getFinalizeCommand();
            return null;
        };
        final ExperimentMethod experimentMethod = new ExperimentMethod();
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
