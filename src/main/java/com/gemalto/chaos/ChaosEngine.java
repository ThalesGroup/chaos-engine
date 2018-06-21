package com.gemalto.chaos;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.admin.enums.AdminState;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChaosEngine {
    public static void main (String[] args) {
        SpringApplication app;
        app = new SpringApplication(ChaosEngine.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run();
        startupComplete();
    }

    private static void startupComplete () {
        AdminManager.setAdminState(AdminState.STARTED);
    }
}
