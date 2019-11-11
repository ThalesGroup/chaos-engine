package com.thales.chaos.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ChaosWebConfigurer implements WebMvcConfigurer {
    @Override
    public void addViewControllers (ViewControllerRegistry registry) {
        registry.addViewController("/help").setViewName("redirect:/help/");
        registry.addViewController("/help/").setViewName("forward:index.html");
        registry.addViewController("/help/**/{[path:[^\\.]*}").setViewName("forward:index.html");
    }
}
