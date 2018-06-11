package com.gemalto.chaos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.notification.NotificationMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gemalto.chaos.platform.Platform;

@Component
public class TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private List<Platform> platforms;

    @Autowired
    private List<NotificationMethods> notificationMethods;


    @Scheduled(cron = "* * * * * *")
    public void task() {


        log.info("The time is now {}", dateFormat.format(new Date()));
        log.info("This is the list of platforms: {}", platforms);
        for (Platform platform : platforms)
        {
            List<Container> containers = platform.getRoster();
            if (containers != null && ! containers.isEmpty()) {
                for (Container container: containers) {
                    platform.destroy(container);
                    sendNotification("Destroyed container " + container);
                }
            }

        }
    }

    private void sendNotification(String event) {
        for (NotificationMethods notif : notificationMethods) {
            notif.logEvent(event);
        }
    }
}
