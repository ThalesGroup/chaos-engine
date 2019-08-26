package com.thales.chaos.experiment;

import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.ChaosErrorCode;
import com.thales.chaos.experiment.enums.ExperimentState;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.platform.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.thales.chaos.constants.DataDogConstants.*;
import static com.thales.chaos.exception.enums.ChaosErrorCode.ANOTHER_EXPERIMENT_IN_PROGRESS;
import static com.thales.chaos.exception.enums.ChaosErrorCode.NOT_ENOUGH_CONTAINERS_FOR_PLANNED_EXPERIMENT;
import static com.thales.chaos.experiment.enums.ExperimentState.*;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static net.logstash.logback.argument.StructuredArguments.*;

@Component
public class ExperimentManager {
    private static final Logger log = LoggerFactory.getLogger(ExperimentManager.class);
    private final Collection<Experiment> allExperiments = new HashSet<>();
    private final Map<Instant, ExperimentSuite> historicalExperimentSuites = new TreeMap<>(Comparator.reverseOrder());
    @Autowired
    private PlatformManager platformManager;
    @Autowired
    private HolidayManager holidayManager;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Scheduled(fixedDelay = 15 * 1000)
    void updateExperimentStatus () {
        synchronized (allExperiments) {
            log.info("Experiments total count: {}", v("count", allExperiments.size()));
            if (!allExperiments.isEmpty()) {
                int experimentCount = Math.min(allExperiments.size(), 64);
                ForkJoinPool threadPool = null;
                try {
                    threadPool = new ForkJoinPool(experimentCount);
                    ForkJoinTask<?> threadPoolSubmit = threadPool.submit(this::evaluateExperiments);
                    threadPoolSubmit.quietlyJoin();
                } finally {
                    if (threadPool != null) threadPool.shutdown();
                }
                if (log.isDebugEnabled()) {
                    calculateExperimentStats();
                }
                allExperiments.removeIf(Experiment::isComplete);
            }
        }
    }

    void calculateExperimentStats () {
        Map<ExperimentState, Long> experimentsInStateCount = allExperiments.stream()
                                                                           .collect(Collectors.groupingBy(Experiment::getExperimentState, Collectors
                                                                                   .counting()));
        Arrays.stream(values())
              .filter(not(experimentsInStateCount::containsKey))
              .forEach(entry -> experimentsInStateCount.put(entry, 0L));
        //noinspection PlaceholderCountMatchesArgumentCount
        log.debug("Experiments count by state", v("experimentState", experimentsInStateCount));
        Map<ExperimentState, List<String>> experimentsByState = allExperiments.stream()
                                                                              .collect(Collectors.groupingBy(Experiment::getExperimentState))
                                                                              .entrySet()
                                                                              .stream()
                                                                              .collect(Collectors.toMap(Map.Entry::getKey, e -> e
                                                                                      .getValue()
                                                                                      .stream()
                                                                                      .map(Experiment::getId)
                                                                                      .sorted(Comparator.naturalOrder())
                                                                                      .collect(toList())));
        //noinspection PlaceholderCountMatchesArgumentCount
        log.debug("Experiments by state", v("experimentState", experimentsByState));
    }

    void evaluateExperiments () {
        allExperiments.parallelStream().forEach(this::runExperimentSteps);
    }

    void runExperimentSteps (Experiment experiment) {
        try (AutoCloseableMDCCollection ignored = getExperimentAutoCloseableMDCCollection(experiment)) {
            if (experiment.getExperimentState().equals(CREATED)) experiment.startExperiment();
            if (experiment.getExperimentState().equals(STARTING)) experiment.confirmStartupComplete();
            if (experiment.getExperimentState().equals(STARTED)) experiment.evaluateRunningExperiment();
            if (experiment.getExperimentState().equals(SELF_HEALING)) experiment.callSelfHealing();
            if (experiment.getExperimentState().equals(FINALIZING)) experiment.callFinalize();
            if (experiment.getExperimentState().equals(FINISHED)) experiment.closeFinishedExperiment();
            if (experiment.getExperimentState().equals(FAILED)) experiment.closeFailedExperiment();
            log.info("Evaluated experiment: {}", v("experiment", experiment));
        }
    }

    AutoCloseableMDCCollection getExperimentAutoCloseableMDCCollection (Experiment experiment) {
        Map<String, String> map = new TreeMap<>(experiment.getContainer().getDataDogTags());
        map.put(DATADOG_PLATFORM_KEY, experiment.getContainer().getPlatform().getPlatformType());
        map.put(DATADOG_EXPERIMENTID_KEY, experiment.getId());
        map.put(DATADOG_EXPERIMENT_METHOD_KEY, experiment.getExperimentMethodName());
        return new AutoCloseableMDCCollection(map);
    }

    @Scheduled(fixedDelay = 1000 * 15)
    void scheduleExperiments () {
        scheduleExperiments(false);
    }

    synchronized Set<Experiment> scheduleExperiments (final boolean force) {
        if (allExperiments.isEmpty()) {
            if (platformManager.getPlatforms().isEmpty()) {
                log.warn("There are no platforms enabled");
                return Collections.emptySet();
            }
            Optional<Platform> eligiblePlatform = platformManager.getNextPlatformForExperiment(force);
            if (eligiblePlatform.isEmpty()) {
                log.debug("No platforms eligible for experiments");
                return Collections.emptySet();
            }
            Platform chosenPlatform = eligiblePlatform.get();
            List<Container> roster = chosenPlatform.scheduleExperiment().generateExperimentRoster();
            if (roster.isEmpty()) {
                log.debug("Platform {} has empty roster, no experiments scheduled", keyValue(DATADOG_PLATFORM_KEY, chosenPlatform
                        .getPlatformType()));
                return Collections.emptySet();
            }
            Set<Container> containersToExperiment;
            do {
                containersToExperiment = roster.parallelStream()
                                               .filter(Container::canExperiment)
                                               .collect(Collectors.toSet());
            } while (force && containersToExperiment.isEmpty());
            synchronized (allExperiments) {
                Set<Experiment> experiments = containersToExperiment.stream()
                                                                    .map(Container::createExperiment)
                                                                    .peek(autowireCapableBeanFactory::autowireBean)
                                                                    .map(this::addExperiment)
                                                                    .peek(experiment -> log.info("Experiment {}, {} added to the queue", experiment
                                                                            .getId(), experiment))
                                                                    .collect(Collectors.toSet());
                logExperimentSuiteEquivalent(chosenPlatform, experiments);
                return experiments;
            }
        }
        return Collections.emptySet();
    }

    private void logExperimentSuiteEquivalent (Platform platform, Set<Experiment> experiments) {
        if (experiments.isEmpty()) return;
        ExperimentSuite experimentSuite = ExperimentSuite.fromExperiments(platform, experiments);
        addExperimentSuiteToHistory(experimentSuite);
        log.info("Experiment can be recreated using {}", kv("experimentSuite", experimentSuite));
    }

    private ExperimentSuite addExperimentSuiteToHistory (ExperimentSuite experimentSuite) {
        return historicalExperimentSuites.put(Instant.now(), experimentSuite);
    }

    Experiment addExperiment (Experiment experiment) {
        allExperiments.add(experiment);
        return experiment;
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

    Experiment getExperimentByUUID (String uuid) {
        return allExperiments.stream().filter(experiment -> experiment.getId().equals(uuid)).findFirst().orElse(null);
    }

    Collection<Experiment> getAllExperiments () {
        return allExperiments;
    }

    Collection<Experiment> scheduleExperimentSuite (ExperimentSuite experimentSuite) {
        return scheduleExperimentSuite(experimentSuite, 1);
    }

    public Map<Instant, ExperimentSuite> getHistoricalExperimentSuites () {
        return historicalExperimentSuites;
    }

    Collection<Experiment> scheduleExperimentSuite (ExperimentSuite experimentSuite, int minimumNumberOfSurvivors) {
        synchronized (allExperiments) {
            if (!allExperiments.isEmpty()) {
                log.warn("Cannot start a planned experiment because another experiment is running");
                throw new ChaosException(ANOTHER_EXPERIMENT_IN_PROGRESS);
            }
            log.info("Request to start a pre-planned experiment with criteria {}", kv("experimentSuite", experimentSuite));
            Platform experimentPlatform = platformManager.getPlatforms()
                                                         .stream()
                                                         .filter(platform -> platform.getPlatformType()
                                                                                     .equals(experimentSuite.getPlatformType()))
                                                         .findFirst()
                                                         .orElseThrow(ChaosErrorCode.PLATFORM_DOES_NOT_EXIST.asChaosException());
            experimentPlatform.expireCachedRoster();
            Collection<Experiment> createdExperiments = experimentSuite.getAggregationIdentifierToExperimentMethodsMap()
                                                                       .entrySet()
                                                                       .stream()
                                                                       .sorted(Comparator.comparingInt(i -> i.getValue()
                                                                                                             .size()))
                                                                       .flatMap((Map.Entry<String, List<String>> containerExperiments) -> createSpecificExperiments(experimentPlatform, containerExperiments
                                                                               .getKey(), containerExperiments.getValue(), minimumNumberOfSurvivors))
                                                                       .peek(experiment -> autowireCapableBeanFactory.autowireBean(experiment))
                                                                       .collect(Collectors.toUnmodifiableSet());
            addExperimentSuiteToHistory(experimentSuite);
            allExperiments.addAll(createdExperiments);
            return allExperiments;
        }
    }

    Stream<Experiment> createSpecificExperiments (Platform platform, String containerAggregationIdentifier, List<String> experimentMethods, int minimumNumberOfSurvivors) {
        log.debug("Creating experiments of type {} against {} with identifier {}", experimentMethods, platform, containerAggregationIdentifier);
        List<Container> potentialContainers = new ArrayList<>(platform.getRosterByAggregationId(containerAggregationIdentifier));
        if (potentialContainers.size() - minimumNumberOfSurvivors < experimentMethods.size()) {
            throw new ChaosException(NOT_ENOUGH_CONTAINERS_FOR_PLANNED_EXPERIMENT);
        }
        Collections.shuffle(potentialContainers);
        return IntStream.range(0, experimentMethods.size()).mapToObj(i -> {
            Container container = potentialContainers.get(i);
            String experimentMethod = experimentMethods.get(i);
            return container.createExperiment(experimentMethod);
        });
    }

    static class AutoCloseableMDCCollection implements AutoCloseable {
        private final Collection<MDC.MDCCloseable> messageDataContextCollection;

        AutoCloseableMDCCollection (Map<String, String> dataContexts) {
            messageDataContextCollection = dataContexts.entrySet()
                                                       .stream()
                                                       .map(entrySet -> MDC.putCloseable(entrySet.getKey(), entrySet.getValue()))
                                                       .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public void close () {
            messageDataContextCollection.forEach(MDC.MDCCloseable::close);
        }
    }
}
