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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_EXPERIMENTID_KEY;
import static com.gemalto.chaos.constants.DataDogConstants.DATADOG_PLATFORM_KEY;
import static net.logstash.logback.argument.StructuredArguments.*;

@Component
public class ExperimentManager {
    private static final Logger log = LoggerFactory.getLogger(ExperimentManager.class);
    private final Collection<Experiment> activeExperiments = new HashSet<>();
    private final Collection<Experiment> newExperimentQueue = new HashSet<>();
    private final Map<Experiment, Future<Boolean>> startedExperiments = new HashMap<>();
    private PlatformManager platformManager;
    private HolidayManager holidayManager;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Autowired
    public ExperimentManager (PlatformManager platformManager, HolidayManager holidayManager) {
        this.platformManager = platformManager;
        this.holidayManager = holidayManager;
    }

    public Map<Experiment, Future<Boolean>> getStartedExperiments () {
        return startedExperiments;
    }

    @Scheduled(fixedDelay = 15 * 1000)
    void updateExperimentStatus () {
        synchronized (activeExperiments) {
            log.debug("Checking on existing experiments");
            if (activeExperiments.isEmpty() && startedExperiments.isEmpty()) {
                log.debug("No experiments are currently active.");
                startNewExperiments();
            } else {
                transitionExperimentsThatHaveStarted();
                updateExperimentStatusImpl();
            }
        }
    }

    void startNewExperiments () {
        if (newExperimentQueue.isEmpty()) return;
        if (holidayManager.isHoliday() || holidayManager.isOutsideWorkingHours()) {
            log.debug("Cannot start new experiments right now: {} ", holidayManager.isHoliday() ? "public holiday" : "out of business");
            return;
        }
        synchronized (newExperimentQueue) {
            startedExperiments.putAll(newExperimentQueue.parallelStream()
                                                        .peek(autowireCapableBeanFactory::autowireBean)
                                                        .collect(Collectors.toMap(Function.identity(), Experiment::startExperiment)));
            transitionExperimentsThatHaveStarted();
            newExperimentQueue.clear();
        }

    }

    void transitionExperimentsThatHaveStarted () {
        Collection<Experiment> finishedStarting = startedExperiments.entrySet()
                                                                    .stream()
                                                                    .filter(experimentFutureEntry -> experimentFutureEntry
                                                                            .getValue()
                                                                            .isDone())
                                                                    .map(Map.Entry::getKey)
                                                                    .collect(Collectors.toSet());
        Collection<Experiment> failedExperiments = startedExperiments.entrySet()
                                                                     .stream()
                                                                     .filter(experimentFutureEntry -> finishedStarting.contains(experimentFutureEntry
                                                                             .getKey()))
                                                                     .filter(experimentFutureEntry -> {
                                                                         try {
                                                                             return !experimentFutureEntry.getValue()
                                                                                                          .get();
                                                                         } catch (InterruptedException e) {
                                                                             Thread.currentThread().interrupt();
                                                                             log.error("Interrupted during asynchronous start of experiment {}", v(DATADOG_EXPERIMENTID_KEY, experimentFutureEntry
                                                                                     .getKey()
                                                                                     .getId()), e);
                                                                             return true;
                                                                         } catch (ExecutionException e) {
                                                                             log.error("Error in asynchronous start of experiment {}", v(DATADOG_EXPERIMENTID_KEY, experimentFutureEntry
                                                                                     .getKey()
                                                                                     .getId()), e);
                                                                             return true;
                                                                         }
                                                                     })
                                                                     .map(Map.Entry::getKey)
                                                                     .collect(Collectors.toSet());
        failedExperiments.addAll(startedExperiments.entrySet()
                                                   .stream()
                                                   .filter(experimentFutureEntry -> experimentFutureEntry.getValue()
                                                                                                         .isCancelled())
                                                   .map(Map.Entry::getKey)
                                                   .collect(Collectors.toSet()));
        finishedStarting.removeAll(failedExperiments);
        activeExperiments.addAll(finishedStarting);
        finishedStarting.forEach(startedExperiments::remove);
        failedExperiments.forEach(startedExperiments::remove);
    }

    private void updateExperimentStatusImpl () {
        log.info("Updating status on active experiments");
        log.info("Active experiments: {}", activeExperiments.size());
        Set<Experiment> finishedExperiments = new HashSet<>();
        activeExperiments.parallelStream().forEach(experiment -> {
            try (MDC.MDCCloseable ignored1 = MDC.putCloseable(DATADOG_PLATFORM_KEY, experiment.getContainer()
                                                                                              .getPlatform()
                                                                                              .getPlatformType())) {
                    try (MDC.MDCCloseable ignored2 = MDC.putCloseable(DATADOG_EXPERIMENTID_KEY, experiment.getId())) {
                        try (MDC.MDCCloseable ignored3 = MDC.putCloseable(DataDogConstants.DATADOG_EXPERIMENT_METHOD_KEY, experiment
                                .getExperimentMethodName())) {
                            experiment.getContainer().setMappedDiagnosticContext();
                            ExperimentState experimentState = experiment.getExperimentState();
                            if (experimentState == ExperimentState.FINISHED || experimentState == ExperimentState.FAILED) {
                                log.info("Removing experiment from active experiment roster, {}, {}", kv("finalExperimentDuration", experiment
                                        .getDuration()), kv("selfHealingRequired", experiment.isSelfHealingRequired()));
                                finishedExperiments.add(experiment);
                            }
                        } finally {
                            experiment.getContainer().clearMappedDiagnosticContext();
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

    Experiment addExperiment (Experiment experiment) {
        newExperimentQueue.add(experiment);
        return experiment;
    }

    Collection<Experiment> getActiveExperiments () {
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
