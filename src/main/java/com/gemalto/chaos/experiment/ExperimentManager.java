package com.gemalto.chaos.experiment;

import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.constants.DataDogConstants;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentState;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_EXPERIMENTID_KEY;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_PLATFORM_KEY;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class ExperimentManager {
    private static final Logger log = LoggerFactory.getLogger(ExperimentManager.class);
    private final Set<Experiment> activeExperiments = new HashSet<>();
    private final Collection<Experiment> newExperimentQueue = new HashSet<>();
    private PlatformManager platformManager;
    private HolidayManager holidayManager;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Autowired
    public ExperimentManager (PlatformManager platformManager, HolidayManager holidayManager) {
        this.platformManager = platformManager;
        this.holidayManager = holidayManager;
    }

    Experiment addExperiment (Experiment experiment) {
            newExperimentQueue.add(experiment);
        return experiment;
    }

    @Scheduled(fixedDelay = 15 * 1000)
    void updateExperimentStatus () {
        synchronized (activeExperiments) {
            log.debug("Checking on existing experiments");
            if (activeExperiments.isEmpty()) {
                log.debug("No experiments are currently active.");
                startNewExperiments();
            } else {
                updateExperimentStatusImpl();
            }
        }
    }

    private void startNewExperiments () {
        if (newExperimentQueue.isEmpty()) return;
        if (holidayManager.isHoliday() || holidayManager.isOutsideWorkingHours()) {
            log.debug("Cannot start new experiments right now: {} ", holidayManager.isHoliday() ? "public holiday" : "out of business");
            return;
        }
        synchronized (newExperimentQueue) {
            newExperimentQueue.parallelStream()
                              .peek(autowireCapableBeanFactory::autowireBean)
                              .peek(experiment -> MDC.put(DATADOG_EXPERIMENTID_KEY, experiment.getId()))
                              .map(experiment -> experiment.startExperiment() ? experiment : null)
                              .peek(experiment -> MDC.remove(DATADOG_EXPERIMENTID_KEY))
                              .filter(Objects::nonNull)
                              .forEach(activeExperiments::add);
            newExperimentQueue.clear();
        }

    }

    private void updateExperimentStatusImpl () {
        log.info("Updating status on active experiments");
        log.info("Active experiments: {}", activeExperiments.size());
        Set<Experiment> finishedExperiments = new HashSet<>();
        activeExperiments.parallelStream().forEach(experiment -> {
            try (MDC.MDCCloseable ignored1 = MDC.putCloseable(DATADOG_PLATFORM_KEY, experiment.getContainer()
                                                                                              .getPlatform()
                                                                                              .getPlatformType())) {
                try (MDC.MDCCloseable ignored = MDC.putCloseable(experiment.getContainer()
                                                                           .getDataDogIdentifier()
                                                                           .getKey(), experiment.getContainer()
                                                                                                .getDataDogIdentifier()
                                                                                                .getValue())) {
                    try (MDC.MDCCloseable ignored2 = MDC.putCloseable(DATADOG_EXPERIMENTID_KEY, experiment.getId())) {
                        try (MDC.MDCCloseable ignored3 = MDC.putCloseable(DataDogConstants.DATADOG_EXPERIMENT_METHOD_KEY, experiment
                                .getExperimentMethodName())) {
                            ExperimentState experimentState = experiment.getExperimentState();
                            if (experimentState == ExperimentState.FINISHED || experimentState == ExperimentState.FAILED) {
                                log.info("Removing experiment from active experiment roster, {}", kv("finalExperimentDuration", experiment
                                        .getDuration()));
                                finishedExperiments.add(experiment);
                            }
                        }
                    }
                }
            }
        });
        activeExperiments.removeAll(finishedExperiments);
    }

    @Scheduled(fixedDelay = 1000 * 15)
    void scheduleExperiments () {
        scheduleExperiments(false);
    }

    synchronized Set<Experiment> scheduleExperiments (final boolean force) {
        if (activeExperiments.isEmpty() && newExperimentQueue.isEmpty()) {
            if (platformManager.getPlatforms().isEmpty()) {
                log.warn("There are no platforms enabled");
                return Collections.emptySet();
            }
            Optional<Platform> eligiblePlatform = platformManager.getPlatforms()
                                                                 .stream()
                                                                 .peek(platform -> platform.usingHolidayManager(holidayManager))
                                                                 .filter(platform1 -> force || platform1.canExperiment())
                                                                 .filter(platform1 -> !platform1.getRoster().isEmpty())
                                                                 .min(Comparator.comparingLong(platform -> platform.getNextChaosTime()
                                                                                                                   .toEpochMilli()));
            if (!eligiblePlatform.isPresent()) {
                log.debug("No platforms eligible for experiments");
                return Collections.emptySet();
            }
            Platform chosenPlatform = eligiblePlatform.get();
            List<Container> roster = chosenPlatform.scheduleExperiment().generateExperimentRoster();
            if (roster.isEmpty()) {
                log.debug("Platform {} has empty roster, no experiments scheduled", keyValue(DATADOG_PLATFORM_KEY, chosenPlatform.getPlatformType()));
                return Collections.emptySet();
            }
            Set<Container> containersToExperiment;
            do {
                containersToExperiment = roster.parallelStream()
                                               .filter(Container::canExperiment)
                                               .collect(Collectors.toSet());
            } while (force && containersToExperiment.isEmpty());
            synchronized (newExperimentQueue) {
                return containersToExperiment.stream()
                                             .map(Container::createExperiment)
                                             .map(this::addExperiment)
                                             .peek(experiment -> log.info("Experiment {}, {} added to the queue", experiment
                                                     .getId(), experiment))
                                             .collect(Collectors.toSet());
            }

        }
        return Collections.emptySet();
    }

    Set<Experiment> getActiveExperiments () {
        return activeExperiments;
    }

    Set<Experiment> experimentContainerId (Long containerIdentity) {
        return platformManager.getPlatforms()
                              .stream()
                              .map(Platform::getRoster)
                              .flatMap(Collection::stream)
                              .filter(container -> container.getIdentity() == containerIdentity)
                              .map(Container::createExperiment)
                              .map(this::addExperiment)
                              .collect(Collectors.toSet());
    }

    Collection<Experiment> getNewExperimentQueue () {
        return newExperimentQueue;
    }

    Experiment getExperimentByUUID (String uuid) {
        return activeExperiments.stream()
                                .filter(experiment -> experiment.getId().equals(uuid))
                                .findFirst()
                                .orElse(null);
    }
}
