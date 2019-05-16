package com.thales.chaos;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.thales.chaos")
@EnableScheduling
@EnableConfigurationProperties
public class ChaosEngine {
    public static void main (String[] args) {
        SpringApplication app;
        app = new SpringApplication(ChaosEngine.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run();
    }
}
