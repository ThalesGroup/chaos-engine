package com.gemalto.chaos;

import com.gemalto.chaos.calendar.HolidayManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.fateengine.FateEngine;
import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.notification.NotificationMethods;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    @Autowired(required = false)
    private List<Platform> platforms;

    @Autowired(required = false)
    private List<NotificationMethods> notificationMethods;

    @Autowired
    private FateEngine fateEngine;

    @Autowired
    private HolidayManager holidayManager;

    private void processPlatformList(List<Platform> platforms) {
        if (platforms != null && !platforms.isEmpty()) {
            for (Platform platform : platforms) {
                try {
                    List<Container> containers = platform.getRoster();
                    processContainerList(containers, platform);
                } catch (Exception e) {
                    log.error("Execution failed while processing {}", platform);
                    log.debug("Details of failure for {}:", platform, e);
                }
            }
        }
    }

    private void processContainerList(List<Container> containers, Platform platform) {
        if (containers != null && !containers.isEmpty()) {
            for (Container container : containers) {
                if (fateEngine.canDestroy(container)) {
                    platform.destroy(container);
                    NotificationManager.sendNotification(ChaosEvent.builder()
                            .withChaosTime(new Date())
                            .withTargetContainer(container)
                            .withMessage("Container terminated")
                            .build());
                }
            }
        }
    }

    /*
    The chaos tools will regularly run on a one-hour schedule, on the hour.
    A custom schedule can be put in place using the 'schedule' environment variable.
     */
    @Scheduled(cron = "${schedule:0 0 * * * *}")
    public void chaosSchedule() {
        if (holidayManager.isHoliday()) {
            log.info("This is no time for chaos.");

        }
        log.info("Using {} to determine container fate", fateEngine.getClass().getSimpleName());

        log.debug("This is the list of platforms: {}", platforms);
        processPlatformList(platforms);
    }
}
