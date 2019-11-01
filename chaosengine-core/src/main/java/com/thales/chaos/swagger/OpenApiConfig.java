package com.thales.chaos.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI () {
        Info info = new Info().title("Chaos Engine API")
                              .description("Controls experiment execution and framework configuration")
                              .license(new License().name("Apache License, Version 2.0")
                                                    .url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
                              .version("1.2");
        return new OpenAPI().components(new Components()).info(info);
    }
}
