package com.gemalto.chaos.experiment;

import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.experiment.enums.ExperimentState;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.platform.Platform;
import com.gemalto.chaos.platform.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Component
public class ExperimentManager {
    private static final Logger log = LoggerFactory.getLogger(ExperimentManager.class);
    private final Set<Experiment> activeExperiments = new HashSet<>();
    private final Queue<Experiment> newExperimentQueue = new LinkedBlockingDeque<>();
    private NotificationManager notificationManager;
    private PlatformManager platformManager;
    private HolidayManager holidayManager;

    @Autowired
    public ExperimentManager (NotificationManager notificationManager, PlatformManager platformManager, HolidayManager holidayManager) {
        this.notificationManager = notificationManager;
        this.platformManager = platformManager;
        this.holidayManager = holidayManager;
    }

    private Experiment addExperiment (Experiment experiment) {
        newExperimentQueue.offer(experiment);
        return experiment;
    }

    @Scheduled(fixedDelay = 15 * 1000)
    synchronized void updateExperimentStatus () {
        synchronized (activeExperiments) {
            startNewExperiments();
            log.debug("Checking on existing experiments");
            if (activeExperiments.isEmpty()) {
                log.debug("No experiments are currently active.");
            } else {
                updateExperimentStatusImpl();
            }
        }
    }

    private void startNewExperiments () {
        if (holidayManager.isHoliday() || holidayManager.isOutsideWorkingHours()) {
            log.debug("Cannot start new experiments right now: {} ", holidayManager.isHoliday() ? "public holiday" : "out of business");
            return;
        }
        Experiment experiment = newExperimentQueue.poll();
        while (experiment != null) {
            if (experiment.startExperiment(notificationManager)) {
                activeExperiments.add(experiment);
            }
            experiment = newExperimentQueue.poll();
        }
    }

    private void updateExperimentStatusImpl () {
        log.info("Updating status on active experiments");
        log.info("Active experiments: {}", activeExperiments.size());
        Set<Experiment> finishedExperiments = new HashSet<>();
        activeExperiments.parallelStream().forEach(experiment -> {
            try (MDC.MDCCloseable ignored1 = MDC.putCloseable("platform", experiment.getContainer()
                                                                                    .getPlatform()
                                                                                    .getPlatformType())) {
                try (MDC.MDCCloseable ignored = MDC.putCloseable(experiment.getContainer()
                                                                           .getDataDogIdentifier()
                                                                           .getKey(), experiment.getContainer()
                                                                                                .getDataDogIdentifier()
                                                                                                .getValue())) {
                    ExperimentState experimentState = experiment.getExperimentState();
                    if (experimentState == ExperimentState.FINISHED) {
                        log.info("Removing experiment {} from active experiment roster", experiment.getId());
                        finishedExperiments.add(experiment);
                    }
                }
            }
        });
        activeExperiments.removeAll(finishedExperiments);
    }

    @Scheduled(fixedDelay = 1000 * 15)
    void startExperiments () {
        startExperiments(false);
    }

    synchronized Queue<Experiment> startExperiments (final boolean force) {
        if (activeExperiments.isEmpty() && newExperimentQueue.isEmpty()) {
            if (platformManager.getPlatforms().isEmpty()) {
                log.warn("There are no platforms enabled");
                return null;
            }
            List<Platform> eligiblePlatforms = platformManager.getPlatforms().stream()
                                                              .peek(platform -> platform.usingHolidayManager(holidayManager))
                                                              .filter(platform1 -> force || platform1.canExperiment())
                                                              .filter(platform1 -> !platform1.getRoster().isEmpty())
                                                              .collect(Collectors.toList());
            if (eligiblePlatforms.isEmpty()) {
                log.debug("No platforms eligible for experiments");
                return null;
            }
            Platform chosenPlatform = eligiblePlatforms.get(new Random().nextInt(eligiblePlatforms.size()));
            List<Container> roster = chosenPlatform.startExperiment().generateExperimentRoster();
            if (roster.isEmpty()) {
                return null;
            }
            Set<Container> containersToExperiment;
            do {
                containersToExperiment = roster.parallelStream()
                                               .filter(Container::canExperiment)
                                               .collect(Collectors.toSet());
            } while (force && containersToExperiment.isEmpty());
            containersToExperiment.stream().map(Container::createExperiment)
                                  .map(this::addExperiment)
                                  .forEach(experiment -> log.info("Experiment {}, {} added to the queue", experiment.getId(), experiment));
            return newExperimentQueue;
        }
        return null;
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

    Queue<Experiment> getNewExperimentQueue () {
        return newExperimentQueue;
    }

    Experiment getExperimentByUUID (String uuid) {
        return activeExperiments.stream()
                                .filter(experiment -> experiment.getId().equals(uuid))
                                .findFirst()
                                .orElse(null);
    }
}
