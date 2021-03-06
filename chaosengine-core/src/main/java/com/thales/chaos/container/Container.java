/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

package com.thales.chaos.container;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.constants.DataDogConstants;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.ExperimentMethod;
import com.thales.chaos.experiment.ExperimentalObject;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentScope;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static com.thales.chaos.exception.enums.ChaosErrorCode.*;
import static com.thales.chaos.util.MethodUtils.getMethodsWithAnnotation;
import static net.logstash.logback.argument.StructuredArguments.v;

public abstract class Container implements ExperimentalObject {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final Map<String, String> dataDogTags = new HashMap<>();
    private final Map<String, Boolean> shellCapabilities = new HashMap<>();
    private ContainerHealth containerHealth;
    private Experiment currentExperiment;

    public Map<String, Boolean> getShellCapabilities () {
        return shellCapabilities;
    }

    @JsonIgnore
    public Map<String, String> getDataDogTags () {
        return dataDogTags;
    }

    @Override
    public boolean canExperiment () {
        if (new Random().nextDouble() < getPlatform().getDestructionProbability()) {
            return eligibleForExperiments();
        }
        log.debug("Cannot experiment on the container right now", v(DataDogConstants.DATADOG_CONTAINER_KEY, this));
        return false;
    }

    @JsonIgnore
    public abstract Platform getPlatform ();

    public boolean eligibleForExperiments () {
        return true;
    }

    public Experiment createExperiment () {
        currentExperiment = GenericContainerExperiment.builder().withContainer(this).build();
        return currentExperiment;
    }

    @Override
    public boolean equals (Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Container other = (Container) o;
        return this.getIdentity() == other.getIdentity();
    }

    @Override
    public int hashCode () {
        List<Field> identifyingFields = getIdentifyingFields();
        AtomicInteger result = new AtomicInteger(0);
        identifyingFields.stream()
                         .map(field -> {
                             try {
                                 return field.get(this);
                             } catch (IllegalAccessException e) {
                                 log.error("Caught IllegalAccessException while evaluating hashcode", e);
                                 return null;
                             }
                         })
                         .mapToInt(o -> Optional.ofNullable(o).map(Object::hashCode).orElse(0))
                         .forEachOrdered(i -> result.set(result.get() * 31 + i));
        return result.get();
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
        final List<Field> identifyingFields = getIdentifyingFields();
        final List<String> identifyingFieldValues = identifyingFields.stream().map(field -> {
            try {
                return field.get(this);
            } catch (IllegalAccessException e) {
                log.error("Caught IllegalAccessException ", e);
                return null;
            }
        }).filter(Objects::nonNull).map(Object::toString).filter(s -> s.length() > 0).collect(Collectors.toUnmodifiableList());
        String identity = String.join("$$$$$", identifyingFieldValues);
        byte[] primitiveByteArray = identity.getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(primitiveByteArray);
        return checksum.getValue();
    }

    private static boolean isIdentifyingField (Field field) {
        return field.getAnnotation(Identifier.class) != null;
    }

    private static int getFieldOrder (Field value) {
        return Optional.of(value).map(field -> field.getAnnotation(Identifier.class)).map(Identifier::order).orElse(Integer.MIN_VALUE + value.getName().hashCode());
    }

    private List<Field> getIdentifyingFields () {
        List<Field> identifyingFields = Arrays.stream(getClass().getDeclaredFields())
                                              .filter(Container::isIdentifyingField)
                                              .sorted(Comparator.comparingInt(field -> field.getName().hashCode()))
                                              .sorted(Comparator.comparingInt(Container::getFieldOrder))
                                              .collect(Collectors.toUnmodifiableList());
        identifyingFields.forEach(f -> f.setAccessible(true));
        return identifyingFields;
    }

    @Override
    public String toString () {
        final List<Field> identifyingFields = getIdentifyingFields();
        List<String> fieldValues = Stream.concat(Stream.of("Container type: " + getClass().getSimpleName()), identifyingFields
                .stream()
                .map(field -> {
                    try {
                        return field.getName() + ":\t" + field.get(this);
                    } catch (IllegalAccessException e) {
                        log.error("Caught IllegalAccessException ", e);
                        return "";
                    }
                })).collect(Collectors.toUnmodifiableList());
        return String.join("\n\t", fieldValues);
    }

    public ContainerHealth getContainerHealth (ExperimentType experimentType) {
        updateContainerHealth(experimentType);
        return containerHealth;
    }

    private void updateContainerHealth (ExperimentType experimentType) {
        containerHealth = updateContainerHealthImpl(experimentType);
    }

    protected abstract ContainerHealth updateContainerHealthImpl (ExperimentType experimentType);

    public Experiment createExperiment (String experimentMethod) {
        currentExperiment = GenericContainerExperiment.builder()
                                                      .withSpecificExperiment(experimentMethod)
                                                      .withContainer(this)
                                                      .build();
        return currentExperiment;
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

    public boolean compareUniqueIdentifier (String uniqueIdentifier) {
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
    public List<Method> getExperimentMethods () {
        return getMethodsWithAnnotation(getClass(), ChaosExperiment.class);
    }

    @SuppressWarnings("unchecked")
    public Runnable recycleCattle () {
        if (!isCattle() || !supportsShellBasedExperiments()) {
            throw new ChaosException(RECYCLING_UNSUPPORTED);
        }
        return () -> {
            getScriptPlatform().recycleContainer(this);
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
        Platform platform = getPlatform();
        try {
            if (platform instanceof ShellBasedExperiment) return (ShellBasedExperiment) platform;
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

    public void clearExperiment () {
        currentExperiment = null;
    }

    public boolean supportsExperimentScope (ExperimentScope experimentScope) {
        return ExperimentScope.MIXED.equals(experimentScope) || (ExperimentScope.PET.equals(experimentScope) ^ isCattle());
    }
}
