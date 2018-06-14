package com.gemalto.chaos;

import com.gemalto.chaos.attack.Attack;
import com.gemalto.chaos.attack.AttackManager;
import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.fateengine.FateEngine;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    @Autowired(required = false)
    private List<Platform> platforms;

    @Autowired
    private FateEngine fateEngine;

    @Autowired
    private HolidayManager holidayManager;

    @Autowired
    private AttackManager attackManager;

    private void processPlatformList(List<Platform> platforms) {
        if (platforms != null && !platforms.isEmpty()) {
            for (Platform platform : platforms) {
                try {
                    List<Container> containers = platform.getRoster();
                    processContainerList(containers);
                } catch (Exception e) {
                    log.error("Execution failed while processing {}", platform);
                    log.error("Details of failure for {}:", platform, e);
                }
            }
        }
    }

    private void processContainerList(List<Container> containers) {
        if (containers != null && !containers.isEmpty()) {
            for (Container container : containers) {
                if (fateEngine.canDestroy(container)) {
                    Attack newAttack = container.createAttack();
                    attackManager.addAttack(newAttack);
                }
            }
        }
    }

    /*
    The chaos tools will regularly run on a one-hour schedule, on the hour.
    A custom schedule can be put in place using the 'schedule' environment variable.
     */
    @Scheduled(cron = "${schedule:0 0 * * * *}")
    void chaosSchedule() {
        if (holidayManager.isHoliday()) {
            log.debug("Dev is on holiday, this is no time for chaos.");
            return;
        } else if (!holidayManager.isWorkingHours()) {
            log.debug("Dev is away, this is no time for chaos.");
            return;
        }
        log.info("Using {} to determine container fate", fateEngine.getClass().getSimpleName());

        log.debug("This is the list of platforms: {}", platforms);
        processPlatformList(platforms);
    }
}
