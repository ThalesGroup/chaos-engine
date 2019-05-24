package com.thales.chaos.container;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.ExperimentMethod;
import com.thales.chaos.experiment.ExperimentalObject;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.experiment.impl.GenericContainerExperiment;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.ShellBasedExperiment;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.shellclient.ShellOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import static com.thales.chaos.exception.enums.ChaosErrorCode.*;
import static com.thales.chaos.util.MethodUtils.getMethodsWithAnnotation;
import static java.util.function.Predicate.not;
import static net.logstash.logback.argument.StructuredArguments.v;

public abstract class Container implements ExperimentalObject {
    protected final transient Logger log = LoggerFactory.getLogger(getClass());
    protected final Map<String, String> dataDogTags = new HashMap<>();
    private final List<ExperimentType> supportedExperimentTypes = new ArrayList<>();
    private final Map<String, Boolean> shellCapabilities = new HashMap<>();
    private ContainerHealth containerHealth;
    private Experiment currentExperiment;

    protected Container () {
        for (ExperimentType experimentType : ExperimentType.values()) {
            if (!getMethodsWithAnnotation(this.getClass(), experimentType.getAnnotation()).isEmpty()) {
                supportedExperimentTypes.add(experimentType);
            }
        }
    }

    public Map<String, Boolean> getShellCapabilities () {
        return shellCapabilities;
    }

    @Override
    public boolean canExperiment () {
        if (!supportedExperimentTypes.isEmpty() && new Random().nextDouble() < getPlatform().getDestructionProbability()) {
            return eligibleForExperiments();
        }
        log.debug("Cannot experiment on the container right now", v(DataDogConstants.DATADOG_CONTAINER_KEY, this));
        return false;
    }

    @JsonIgnore
    public abstract Platform getPlatform ();

    @JsonIgnore
    public List<ExperimentType> getSupportedExperimentTypes () {
        return supportedExperimentTypes;
    }

    @Override
    public boolean equals (Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Container other = (Container) o;
        return this.getIdentity() == other.getIdentity();
    }

    /**
     * Uses all the fields in the container implementation (but not the Container parent class)
     * to create a checksum of the container. This checksum should be immutable and can be used
     * to recognize when building a roster if the container already exists, and can reference
     * the same object.
     *
     * @return A checksum (format long) of the class based on the implementation specific fields
     */
    public long getIdentity () {
        StringBuilder identity = new StringBuilder();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);
            try {
                if (field.get(this) != null) {
                    if (identity.length() > 1) {
                        identity.append("$$$$$");
                    }
                    identity.append(field.get(this).toString());
                }
            } catch (IllegalAccessException e) {
                log.error("Caught IllegalAccessException ", e);
            }
        }
        byte[] primitiveByteArray = identity.toString().getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(primitiveByteArray);
        return checksum.getValue();
    }

    @Override
    public String toString () {
        StringBuilder output = new StringBuilder();
        output.append("Container type: ");
        output.append(this.getClass().getSimpleName());
        Arrays.stream(this.getClass().getDeclaredFields())
              .filter(not(field -> Modifier.isTransient(field.getModifiers())))
              .filter(not(Field::isSynthetic))
              .forEachOrdered(field -> {
                  field.setAccessible(true);
                  try {
                      output.append("\n\t");
                      output.append(field.getName());
                      output.append(":\t");
                      output.append(field.get(this));
                  } catch (IllegalAccessException e) {
                      log.error("Could not read from field {}", field.getName(), e);
                  }
              });
        return output.toString();
    }

    public boolean supportsExperimentType (ExperimentType experimentType) {
        return supportedExperimentTypes.contains(experimentType);
    }

    public ContainerHealth getContainerHealth (ExperimentType experimentType) {
        updateContainerHealth(experimentType);
        return containerHealth;
    }

    private void updateContainerHealth (ExperimentType experimentType) {
        containerHealth = updateContainerHealthImpl(experimentType);
    }

    protected abstract ContainerHealth updateContainerHealthImpl (ExperimentType experimentType);

    public Experiment createExperiment () {
        currentExperiment = createExperiment(supportedExperimentTypes.get(new Random().nextInt(supportedExperimentTypes.size())));
        return currentExperiment;
    }

    public Experiment createExperiment (ExperimentType experimentType) {
        return GenericContainerExperiment.builder().withExperimentType(experimentType).withContainer(this).build();
    }

    public void startExperiment (Experiment experiment) {
        containerHealth = ContainerHealth.RUNNING_EXPERIMENT;
        log.info("Starting a experiment {} against container {}", experiment.getId(), this);
        experimentWithAnnotation(experiment);
    }

    @SuppressWarnings("unchecked")
    private void experimentWithAnnotation (Experiment experiment) {
        try {
            ExperimentMethod lastExperimentMethod = experiment.getExperimentMethod();
            lastExperimentMethod.accept(this, experiment);
        } catch (Exception e) {
            final ChaosException chaosException = new ChaosException(EXPERIMENT_START_FAILURE, e);
            log.error("Failed to run experiment {} on container {}: {}", experiment.getId(), this, chaosException);
            throw chaosException;
        }
    }

    public abstract String getSimpleName ();

    public abstract String getAggregationIdentifier ();

    public String getContainerType () {
        return this.getClass().getSimpleName();
    }

    @JsonIgnore
    public Duration getMinimumSelfHealingInterval () {
        return getPlatform().getMinimumSelfHealingInterval();
    }

    @JsonIgnore
    public abstract DataDogIdentifier getDataDogIdentifier ();

    boolean compareUniqueIdentifier (String uniqueIdentifier) {
        return uniqueIdentifier != null && compareUniqueIdentifierInner(uniqueIdentifier);
    }

    protected abstract boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier);

    public void setMappedDiagnosticContext () {
        dataDogTags.forEach(MDC::put);
    }

    public void clearMappedDiagnosticContext () {
        dataDogTags.keySet().forEach(MDC::remove);
    }

    @JsonIgnore
    public Map<Class<? extends Annotation>, List<Method>> getExperimentMethods () {
        return Arrays.stream(ExperimentType.values())
                     .map(ExperimentType::getAnnotation)
                     .collect(Collectors.toMap(Function.identity(), k -> getMethodsWithAnnotation(this.getClass(), k)));
    }

    @SuppressWarnings("unchecked")
    public Callable<Void> recycleCattle () {
        if (!isCattle() || !supportsShellBasedExperiments()) {
            throw new ChaosException(RECYCLING_UNSUPPORTED);
        }
        return () -> {
            getScriptPlatform().recycleContainer(this);
            return null;
        };
    }

    /**
     * @return Return true if the container can be treated as cattle and destroyed or replaced as necessary
     */
    public boolean isCattle () {
        return false;
    }

    public boolean supportsShellBasedExperiments () {
        return getPlatform() instanceof ShellBasedExperiment;
    }

    private ShellBasedExperiment getScriptPlatform () {
        return getScriptPlatform(this.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T extends Container> ShellBasedExperiment getScriptPlatform (Class<T> ignored) {
        Platform platform = getPlatform();
        try {
            if (platform instanceof ShellBasedExperiment) return (ShellBasedExperiment<T>) platform;
        } catch (ClassCastException e) {
            throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL, e);
        }
        throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL);
    }

    @SuppressWarnings("unchecked")
    public ShellOutput runCommand (String command) {
        if (!supportsShellBasedExperiments()) throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL);
        return getScriptPlatform().runCommand(this, command);
    }

    @JsonIgnore
    public Boolean isContainerRecycled () {
        if (!isCattle()) return Boolean.FALSE;
        return getPlatform().isContainerRecycled(this);
    }

    @SuppressWarnings("unchecked")
    public String runScript (Script script) {
        if (!supportsShellBasedExperiments()) throw new ChaosException(PLATFORM_DOES_NOT_SUPPORT_SHELL);
        return getScriptPlatform().runScript(this, script);
    }

    public Instant getExperimentStartTime () {
        return currentExperiment != null ? currentExperiment.getStartTime() : null;
    }

    public Collection<String> getKnownMissingCapabilities () {
        return shellCapabilities.entrySet()
                                .stream()
                                .filter(stringBooleanEntry -> !Boolean.TRUE.equals(stringBooleanEntry.getValue()))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
    }

    public boolean eligibleForExperiments () {
        return true;
    }
}
