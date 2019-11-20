package com.thales.chaos.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnResource(resources = "classpath:/static/help/index.html")
public class ChaosWebHelpConfigurer implements WebMvcConfigurer {
    @Override
    public void addViewControllers (ViewControllerRegistry registry) {
        registry.addViewController("/help").setViewName("redirect:/help/");
        registry.addViewController("/help/").setViewName("forward:index.html");
        registry.addViewController("/help/**/{[path:[^\\.]*}").setViewName("forward:index.html");
    }

    @Override
    public void addResourceHandlers (ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/help/**").addResourceLocations("classpath:/static/help/");
    }
}
