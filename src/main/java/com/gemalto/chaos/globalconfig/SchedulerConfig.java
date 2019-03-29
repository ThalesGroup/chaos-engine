package com.gemalto.chaos.globalconfig;

import com.gemalto.chaos.ChaosEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Arrays;

@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    private static final int POOL_SIZE = 10;

    @Override
    public void configureTasks (ScheduledTaskRegistrar scheduledTaskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix("chaos-");
        threadPoolTaskScheduler.setErrorHandler(ex -> {
            final Logger logger = Arrays.stream(ex.getStackTrace())
                                        .map(StackTraceElement::getClassName)
                                        .filter(s -> s.startsWith("com.gemalto.chaos"))
                                        .findFirst()
                                        .map(LoggerFactory::getLogger)
                                        .orElseGet(() -> LoggerFactory.getLogger(ChaosEngine.class));
            logger.error("Unhandled ChaosException in Scheduled Thread", ex);
        });
        threadPoolTaskScheduler.initialize();
        scheduledTaskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }
}
