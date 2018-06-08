package com.gemalto.chaos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gemalto.chaos.platform.Platform;

@Component
public class TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private List<Platform> platforms;


    @Scheduled(cron = "0 0 * * * *")
    public void task() {


        log.info("The time is now {}", dateFormat.format(new Date()));
        log.info("This is the list of platforms: {}", platforms);
    }
}
