package com.gemalto.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChaosEngine
{
    private static final Logger log = LoggerFactory.getLogger(ChaosEngine.class);

    private static SpringApplication app;

    public static void main( String[] args )
    {
        app = new SpringApplication(ChaosEngine.class);
        app.setBannerMode(Banner.Mode.OFF);
        ApplicationContext context = app.run();

    }
}
